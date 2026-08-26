#!/usr/bin/env python3
"""
Phân tích GHÉP CẶP cho braincompare_perseed.csv (numpy thuần, không cần scipy).

Thiết kế thí nghiệm là matched-pairs: cùng seed -> cùng layout thế giới cho cả 3 não,
nên so sánh đúng phải dùng kiểm định ghép cặp (Wilcoxon signed-rank) chứ không phải
unpaired t-test. Script in: Wilcoxon (chính), paired t (xấp xỉ chuẩn), Cohen's dz,
số seed thắng, và bootstrap 95% CI cho hiệu trung bình & trung vị.

Dùng:  python3 stats_braincompare.py [csv] [metric] [B_bootstrap]
       metric mặc định = catches_per_1000 ; B mặc định = 20000
"""
import sys, math
import numpy as np

CSV    = sys.argv[1] if len(sys.argv) > 1 else "braincompare_perseed.csv"
METRIC = sys.argv[2] if len(sys.argv) > 2 else "catches_per_1000"
B      = int(sys.argv[3]) if len(sys.argv) > 3 else 20000

def phi(x):                      # CDF chuẩn tắc qua erf
    return 0.5 * (1.0 + math.erf(x / math.sqrt(2.0)))

def load(csv, metric):
    rows = [l.strip().split(",") for l in open(csv) if l.strip()]
    hdr = rows[0]; si, bi, mi = hdr.index("seed"), hdr.index("brain"), hdr.index(metric)
    data = {}  # brain -> {seed: value}
    for r in rows[1:]:
        data.setdefault(r[bi], {})[int(r[si])] = float(r[mi])
    return data

def paired(a_map, b_map):
    seeds = sorted(set(a_map) & set(b_map))
    a = np.array([a_map[s] for s in seeds]); b = np.array([b_map[s] for s in seeds])
    return a, b

def wilcoxon_signed_rank(d):
    """Two-sided Wilcoxon signed-rank, xấp xỉ chuẩn có hiệu chỉnh tie + liên tục.
       Bỏ các hiệu = 0 (quy ước Wilcoxon)."""
    d = d[d != 0]
    n = len(d)
    if n == 0: return float("nan"), float("nan"), 0
    absd = np.abs(d)
    order = np.argsort(absd, kind="mergesort")
    ranks = np.empty(n); i = 0
    sa = absd[order]
    while i < n:                 # average-rank cho tie
        j = i
        while j + 1 < n and sa[j + 1] == sa[i]: j += 1
        ranks[order[i:j + 1]] = (i + j) / 2.0 + 1.0
        i = j + 1
    Wp = ranks[d > 0].sum(); Wm = ranks[d < 0].sum()
    meanW = n * (n + 1) / 4.0
    varW  = n * (n + 1) * (2 * n + 1) / 24.0
    # hiệu chỉnh tie
    _, counts = np.unique(absd, return_counts=True)
    varW -= (counts ** 3 - counts).sum() / 48.0
    if varW <= 0: return float("nan"), Wp, n
    z = (Wp - meanW - math.copysign(0.5, Wp - meanW)) / math.sqrt(varW)
    p = 2.0 * (1.0 - phi(abs(z)))
    return p, z, n

def boot_ci(d, stat, B, alpha=0.05):
    n = len(d); idx = np.random.randint(0, n, size=(B, n))
    samp = stat(d[idx], axis=1)
    lo, hi = np.percentile(samp, [100 * alpha / 2, 100 * (1 - alpha / 2)])
    return lo, hi

def compare(name, a, b):
    d = a - b; n = len(d)
    md, mdn = d.mean(), np.median(d)
    sd = d.std(ddof=1)
    dz = md / sd if sd > 0 else float("nan")
    # paired t (xấp xỉ chuẩn vì df=n-1 lớn)
    t  = md / (sd / math.sqrt(n)) if sd > 0 else float("nan")
    pt = 2.0 * (1.0 - phi(abs(t)))
    pw, z, nr = wilcoxon_signed_rank(d)
    wins = int((d > 0).sum()); ties = int((d == 0).sum()); losses = int((d < 0).sum())
    ci_mean = boot_ci(d, np.mean, B)
    ci_med  = boot_ci(d, np.median, B)
    print(f"\n== {name}  (n={n} cặp) ==")
    print(f"  hiệu TB (A-B)      : {md:+.3f}   (trung vị {mdn:+.3f})")
    print(f"  bootstrap 95% CI TB: [{ci_mean[0]:+.3f}, {ci_mean[1]:+.3f}]   "
          f"-> {'KHÔNG chứa 0 (có ý nghĩa)' if ci_mean[0] * ci_mean[1] > 0 else 'CHỨA 0 (không ý nghĩa)'}")
    print(f"  bootstrap 95% CI TV: [{ci_med[0]:+.3f}, {ci_med[1]:+.3f}]")
    print(f"  Wilcoxon signed-rank: z={z:+.3f}, p={pw:.2e}   (n hiệu khác 0 = {nr})")
    print(f"  paired t (≈chuẩn)   : t={t:+.3f}, p={pt:.2e}")
    print(f"  Cohen's dz (paired) : {dz:+.3f}")
    print(f"  thắng/hòa/thua (A>B): {wins} / {ties} / {losses}")

def main():
    data = load(CSV, METRIC)
    brains = [b for b in ("MC", "QL", "RBS") if b in data]
    print(f"CSV={CSV}  metric={METRIC}  bootstrap B={B}")
    for b in brains:
        vals = np.array(list(data[b].values()))
        print(f"  {b:>3}: mean {vals.mean():.3f}  sd {vals.std(ddof=1):.3f}  "
              f"SE {vals.std(ddof=1)/math.sqrt(len(vals)):.3f}  n={len(vals)}")
    pairs = [("MC", "QL"), ("MC", "RBS"), ("QL", "RBS")]
    for x, y in pairs:
        if x in data and y in data:
            a, b = paired(data[x], data[y]); compare(f"{x} − {y}", a, b)

if __name__ == "__main__":
    main()
