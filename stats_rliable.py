#!/usr/bin/env python3
"""
Đánh giá kiểu rliable (Agarwal et al., NeurIPS 2021) trên braincompare_perseed.csv.

In: Median / IQM / Mean kèm 95% stratified-bootstrap CI cho từng não, và
Probability of Improvement P(A>B) kèm CI cho các cặp. Thiết kế là matched-pairs
(cùng seed -> cùng layout) nên in thêm tỉ-lệ-thắng ghép cặp để đối chiếu.

Dùng:  python3 stats_rliable.py [csv] [metric] [reps]
       metric mặc định = catches_per_1000
Cần:   pip install rliable   (chỉ dùng rliable.metrics — định nghĩa IQM/Prob-of-Improvement
       THẬT của Agarwal et al.; CI tự bootstrap bằng numpy để né rliable.library/arch,
       gói arch hiện vỡ trên Python 3.14).
"""
import sys
import numpy as np
from rliable import metrics

def boot_ci(stat_fn, B, n, rng, *samples, alpha=0.05):
    """Percentile bootstrap CI cho stat_fn áp lên các mẫu (resample run có hoàn lại)."""
    vals = np.empty(B)
    for r in range(B):
        rs = [s[rng.integers(0, len(s), len(s))] for s in samples]
        vals[r] = stat_fn(*rs)
    lo, hi = np.percentile(vals, [100 * alpha / 2, 100 * (1 - alpha / 2)])
    return float(lo), float(hi)

CSV    = sys.argv[1] if len(sys.argv) > 1 else "braincompare_perseed.csv"
METRIC = sys.argv[2] if len(sys.argv) > 2 else "catches_per_1000"
REPS   = int(sys.argv[3]) if len(sys.argv) > 3 else 10000

def load(csv, metric):
    rows = [l.strip().split(",") for l in open(csv) if l.strip()]
    hdr = rows[0]; si, bi, mi = hdr.index("seed"), hdr.index("brain"), hdr.index(metric)
    d = {}
    for r in rows[1:]:
        d.setdefault(r[bi], {})[int(r[si])] = float(r[mi])
    return d

def main():
    data = load(CSV, METRIC)
    order = [b for b in ("MC", "QL", "RBS") if b in data]
    seeds = sorted(set.intersection(*[set(data[b]) for b in order]))
    # scores[algo] shape (num_runs, num_tasks=1)  — quy ước rliable, cao = tốt
    scores = {b: np.array([data[b][s] for s in seeds]).reshape(-1, 1) for b in order}
    n = len(seeds)
    rng = np.random.default_rng(0)
    print(f"CSV={CSV}  metric={METRIC}  n={n} seed (ghép cặp)  reps={REPS}\n")

    # ---- Median / IQM / Mean + 95% bootstrap CI ----
    aggs = [("Median", metrics.aggregate_median),
            ("IQM",    metrics.aggregate_iqm),
            ("Mean",   metrics.aggregate_mean)]
    print("Tổng hợp (điểm [95% CI]) — IQM là chỉ số rliable khuyến nghị:")
    for b in order:
        flat = scores[b][:, 0]
        seg = []
        for nm, fn in aggs:
            pt = float(fn(scores[b]))
            lo, hi = boot_ci(lambda s: float(fn(s.reshape(-1, 1))), REPS, n, rng, flat)
            seg.append(f"{nm} {pt:.2f} [{lo:.2f},{hi:.2f}]")
        print(f"  {b:>3}: " + "  ".join(seg))

    # ---- Probability of improvement P(A>B) + CI (rliable metric, CI tự bootstrap) ----
    print("\nProbability of Improvement  P(A>B)  [95% CI]   (0.5 = không khác):")
    poi = lambda a, b: float(metrics.probability_of_improvement(a.reshape(-1, 1), b.reshape(-1, 1)))
    for x, y in [("MC", "QL"), ("MC", "RBS"), ("QL", "RBS")]:
        if x not in scores or y not in scores:
            continue
        a, b = scores[x][:, 0], scores[y][:, 0]
        pi = poi(a, b)
        lo, hi = boot_ci(poi, REPS, n, rng, a, b)
        wr = float(np.mean(a > b) + 0.5 * np.mean(a == b))     # win-rate ghép cặp
        sig = "✓ có ý nghĩa" if (lo > 0.5 or hi < 0.5) else "≈ CI chứa 0.5"
        print(f"  P({x}>{y}) = {pi:.3f} [{lo:.3f},{hi:.3f}]   | win-rate ghép cặp = {wr:.3f}   {sig}")

    print("\nGhi chú: P(A>B) của rliable là Mann-Whitney (coi các run độc lập); "
          "win-rate ghép cặp khai thác cùng-seed. Optimality gap cần điểm chuẩn-hoá "
          "[0,1] theo oracle/trần nên bỏ ở đây (chưa đo trần môi trường).")

if __name__ == "__main__":
    main()
