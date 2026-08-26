#!/usr/bin/env bash
# P1 — TÁI ĐẤU SÒNG PHẲNG: port rl_struct sang MC (state đói 4-mức/khát 3-mức + phạt đói
# + phạt-dốc khát + gamma 0.99) rồi train MC sói BẰNG ĐÚNG protocol QL ở run_ql_struct.sh
# (1 pha sói, 800 ep x 800 step, từ bảng rỗng) -> BrainCompare 50 + rliable.
# Dùng classpath snapshot để IDE auto-build không wipe target/classes giữa chừng.
set -uo pipefail
cd "$(dirname "$0")"

SNAP=/tmp/mcstruct_classes
rm -rf "$SNAP" && cp -r target/classes "$SNAP"
CP="$SNAP"
for j in $(find "$HOME/.m2/repository/org/openjfx" -name '*.jar' 2>/dev/null | grep -vE 'sources|javadoc'); do CP="$CP:$j"; done

echo ">> backup bảng MC cũ (1-bit) + xoá để train mới (encode đổi -> phải train từ rỗng)"
mkdir -p qtables_backup
cp -f qtables/mc_wolf.qtable   "qtables_backup/mc_wolf_1bit_$(date +%H%M%S).qtable"   2>/dev/null || true
cp -f qtables/mc_rabbit.qtable "qtables_backup/mc_rabbit_1bit_$(date +%H%M%S).qtable" 2>/dev/null || true
rm -f qtables/mc_wolf.qtable

echo ">> TRAIN sói MC: gamma 0.99, 800 ep x 800 step (KHỚP QL) $(date +%H:%M:%S)"
java -cp "$CP" org.openjfx.app.core.montecarlo.MonteCarloTrainer 800 800 wolf 2>&1 | tee train_mc_struct.log

echo ">> EVAL: BrainCompare 50 x 3000 $(date +%H:%M:%S)"
java -cp "$CP" org.openjfx.app.core.BrainCompare 50 3000 2>&1 | tee braincompare_v9.log

echo ">> STATS rliable $(date +%H:%M:%S)"
python3 stats_rliable.py braincompare_perseed.csv catches_per_1000 10000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v9.txt

echo ">> ALL DONE $(date +%H:%M:%S)"
