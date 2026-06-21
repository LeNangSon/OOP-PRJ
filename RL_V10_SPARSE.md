# rl_v10 — Reward THƯA (sparse) cho QL & MC

Bản thí nghiệm tách riêng trên nhánh `rl_v10`: bỏ toàn bộ shaping dày, chỉ giữ tín hiệu
**rời rạc theo sự kiện** để làm bài toán credit-assignment khó → lộ rõ chênh lệch **Monte Carlo
vs Q-learning**.

| | Sự kiện | Reward |
|---|---|---|
| Sói (PREDATOR) | bắt được mồi | **+10** |
| Thỏ (PREY) | ăn được cỏ | **+1** |
| Cả hai | chết | **−10** (đặt ở `learnTerminal`) |
| | mọi bước khác | **0** |

Sửa ở `stepReward()` của [QLearningStrategy](src/main/java/org/openjfx/app/core/strategies/QLearningStrategy.java)
và [MonteCarloStrategy](src/main/java/org/openjfx/app/core/strategies/MonteCarloStrategy.java).
**State format không đổi** → bảng cũ vẫn nạp được, nhưng để thí nghiệm sạch phải train **từ bảng rỗng**.

> Kỳ vọng: dưới reward thưa, **MC bỏ xa QL** hơn so với bản dense (MC lan return cả episode trong 1
> lượt; QL phải "thấm" `+10/−10` ngược 1-hop/episode qua `gamma^horizon`). Vì đã bỏ phạt đói,
> reward-hacking "cắm trại ở hồ" có thể tái xuất ở **sói QL** (chết-đói tăng, catches/ep thấp).

---

## Lệnh train

Tất cả chạy từ thư mục gốc dự án:

```bash
cd "/Volumes/DEV/OOP JAVA/PRJ/OOP-PRJ"
git switch rl_v10           # đảm bảo đang ở nhánh sparse
```

### Bước 1 — sao lưu bảng dense rồi xoá (train lại từ đầu)

`qtables/` được git theo dõi; trainer load-or-resume (bảng rỗng → epsilon khởi đầu = 1.0 = học từ đầu;
bảng có sẵn → 0.3 = học tiếp, sẽ lẫn với reward dense cũ). Nên phải xoá:

```bash
mkdir -p qtables_dense && cp qtables/*.qtable qtables_dense/
rm -f qtables/wolf.qtable qtables/rabbit.qtable qtables/mc_wolf.qtable qtables/mc_rabbit.qtable
```

### Bước 2 — train MC sparse (pipeline xen kẽ: sói 800 → thỏ 800 → sói tinh chỉnh 400, maxSteps 400)

```bash
./train_mc.sh 2>&1 | tee train_mc_sparse.log
```
→ tạo `qtables/mc_wolf.qtable` + `qtables/mc_rabbit.qtable`

### Bước 3 — train QL sparse (cùng pipeline, maxSteps 600)

```bash
./train.sh 2>&1 | tee train_ql_sparse.log
```
→ tạo `qtables/wolf.qtable` + `qtables/rabbit.qtable`

### (Tuỳ chọn) Chạy 1 pha thủ công — nhanh, chỉ sói

```bash
./train_mc.sh 800 400 wolf 2>&1 | tee train_mc_sparse.log   # chỉ sói MC
./train.sh    800 600 wolf 2>&1 | tee train_ql_sparse.log   # chỉ sói QL
```
Mode: `wolf` | `rabbit` | `wolfql` | `rbs`.

### Chạy nền (không chiếm terminal)

```bash
nohup ./train_mc.sh > train_mc_sparse.log 2>&1 &
nohup ./train.sh    > train_ql_sparse.log 2>&1 &
```

---

## So sánh định lượng (sau khi train)

So sói **RBS vs QL vs MC** (thỏ RBS), mặc định 20 seed × 3000 step:

```bash
CP="target/classes"; for j in $(find ~/.m2/repository/org/openjfx -name '*.jar' | grep -vE 'sources|javadoc'); do CP="$CP:$j"; done
java -cp "$CP" org.openjfx.app.core.BrainCompare 20 3000 2>&1 | tee braincompare_sparse.log
```

## Xem trực quan trong UI

```bash
./mvnw -o javafx:run -Dmc=true     # sói/thỏ MC sparse
./mvnw -o javafx:run -Dql=true     # sói/thỏ QL sparse
```

## Khôi phục bản dense

```bash
cp qtables_dense/*.qtable qtables/
```

---

## Hướng dẫn xem log

### Lúc đang train (theo dõi tiến độ trực tiếp)

Trainer in mỗi `episodes/50` dòng dạng:
```
ep    800 | eps 0.05 | catches/ep 6.80 | wolfStates 291 | rabbitStates 0
```
- `catches/ep` = số thỏ bị bắt/episode (chỉ số chính để thấy não đang khá lên).
- `eps` giảm dần (1.0 → 0.05): khám phá → khai thác.
- `wolfStates`/`rabbitStates` = số trạng thái trong Q-table (tăng dần khi gặp tình huống mới).

Nếu chạy nền (`nohup ... &`), bám đuôi log để xem realtime:
```bash
tail -f train_mc_sparse.log        # Ctrl-C để thôi bám (không dừng train)
```

Chỉ xem các dòng tiến độ (lọc bỏ warning JVM/Maven):
```bash
tail -f train_mc_sparse.log | grep --line-buffered -E "^ep |^MC|^Xong|catches/ep"
```

### Sau khi train xong

Dòng tổng kết của mỗi pha:
```bash
grep -E "Xong|DO CHUAN|catches/ep" train_mc_sparse.log train_ql_sparse.log
```
Ví dụ kết quả cuối: `Xong 42.1s (catches/ep 6.80). Luu .../mc_wolf.qtable (291 trang thai)`.

So MC vs QL nhanh — chỉ lấy số catches/ep cuối mỗi log:
```bash
for f in train_mc_sparse.log train_ql_sparse.log; do echo "== $f =="; grep "catches/ep" "$f" | tail -1; done
```

### Đọc log so sánh

`BrainCompare` in bảng tổng kết ở cuối (catches, chết đói/khát/săn, first-catch...). Xem phần báo cáo:
```bash
sed -n '/====/,$p' braincompare_sparse.log     # hoặc mở thẳng file, bảng nằm ở cuối
```
Điểm cần soi cho thí nghiệm sparse: **MC catches/ep > QL** rõ rệt, và **sói QL chết-đói cao** (dấu hiệu
camp/reward-hacking quay lại khi bỏ phạt đói).

### Log UI

UI ghi ra `ui_mc.log` / `ui_ql.log` (nếu chạy kèm `| tee`). Kiểm tra não đã nạp đúng:
```bash
grep -E "Monte Carlo|Q-learning|BAT|trang thai" ui_mc.log
```

### Dọn log

```bash
rm -f train_*_sparse.log braincompare_sparse.log
```
