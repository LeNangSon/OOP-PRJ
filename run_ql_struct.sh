#!/usr/bin/env bash
# rl_struct: train sói QL với gamma 0.99 + state đói 4-mức + phạt-dốc đã hạ (2.0->0.4),
# rồi đánh giá BrainCompare 50 + rliable. Chạy từ classpath snapshot để IDE auto-build
# không wipe target/classes giữa chừng (đã từng làm hỏng run trước).
set -uo pipefail
cd "$(dirname "$0")"

SNAP=/tmp/qlstruct_classes
rm -rf "$SNAP" && cp -r target/classes "$SNAP"
CP="$SNAP"
for j in $(find "$HOME/.m2/repository/org/openjfx" -name '*.jar' 2>/dev/null | grep -vE 'sources|javadoc'); do CP="$CP:$j"; done

echo ">> backup bảng phạt-dốc + xoá để train mới (encode đổi -> phải train từ rỗng)"
mkdir -p qtables_backup
cp -f qtables/wolf.qtable "qtables_backup/wolf_phatdoc_$(date +%H%M%S).qtable" 2>/dev/null || true
rm -f qtables/wolf.qtable

echo ">> TRAIN sói: gamma 0.99, 800 ep x 800 step $(date +%H:%M:%S)"
java -cp "$CP" org.openjfx.app.core.qlearning.QLearningTrainer 800 800 wolf 2>&1 | tee train_ql_struct.log

echo ">> EVAL: BrainCompare 50 $(date +%H:%M:%S)"
java -cp "$CP" org.openjfx.app.core.BrainCompare 50 3000 2>&1 | tee braincompare_v8.log

echo ">> STATS rliable $(date +%H:%M:%S)"
python3 stats_rliable.py braincompare_perseed.csv catches_per_1000 10000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v8.txt

echo ">> ALL DONE $(date +%H:%M:%S)"
