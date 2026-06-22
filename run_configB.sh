#!/usr/bin/env bash
# KHỐI B — "reward tốt nhất của MC" (cấu hình v5): state THÔ (1-bit) + reward LEAN (bỏ phạt đói /
# phạt-dốc khát nguy cấp) + gamma 0.97. Train CẢ HAI não trên cấu hình này rồi eval 50 seed 3-não,
# để có best-of-MC (MC_B) VÀ xem QL chạy trên reward của MC (QL_B).
# Đối chiếu: KHỐI A = reward tốt nhất của QL (rl_struct) đã có ở braincompare_v9 (QL 7.04 / MC 3.62).
set -uo pipefail
cd "$(dirname "$0")"

SNAP=/tmp/cb_classes
rm -rf "$SNAP" && cp -r target/classes "$SNAP"
CP="$SNAP"
for j in $(find "$HOME/.m2/repository/org/openjfx" -name '*.jar' 2>/dev/null | grep -vE 'sources|javadoc'); do CP="$CP:$j"; done

QL=org.openjfx.app.core.qlearning.QLearningTrainer
MC=org.openjfx.app.core.montecarlo.MonteCarloTrainer
BC=org.openjfx.app.core.BrainCompare
FLAGS_TRAIN="-Dlean=true -Dgamma=0.97"
mkdir -p qtables_backup

echo "############ KHỐI B (MC native: thô + lean + γ0.97) — $(date +%H:%M:%S) ############"
cp -f qtables/wolf.qtable    "qtables_backup/wolf_fine_cb_$(date +%H%M%S).qtable"    2>/dev/null || true
cp -f qtables/mc_wolf.qtable "qtables_backup/mc_wolf_fine3000_cb_$(date +%H%M%S).qtable" 2>/dev/null || true
rm -f qtables/wolf.qtable qtables/mc_wolf.qtable

echo ">> TRAIN QL_B: -DcoarseQL $FLAGS_TRAIN, 800x800 (từ rỗng) $(date +%H:%M:%S)"
java -DcoarseQL=true $FLAGS_TRAIN -cp "$CP" $QL 800 800 wolf 2>&1 | tee train_ql_configB.log
echo ">> TRAIN MC_B: -DcoarseMC $FLAGS_TRAIN, 800x800 (từ rỗng) $(date +%H:%M:%S)"
java -DcoarseMC=true $FLAGS_TRAIN -cp "$CP" $MC 800 800 wolf 2>&1 | tee train_mc_configB.log

echo ">> EVAL v13 (-DcoarseQL -DcoarseMC: cả QL lẫn MC dùng state thô khớp bảng) $(date +%H:%M:%S)"
java -DcoarseQL=true -DcoarseMC=true -cp "$CP" $BC 50 3000 2>&1 | tee braincompare_v13.log
cp -f braincompare_perseed.csv braincompare_perseed_v13.csv
python3 stats_rliable.py braincompare_perseed.csv catches_per_1000 10000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v13.txt
python3 stats_braincompare.py braincompare_perseed.csv catches_per_1000 20000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v13_paired.txt

echo "############ KHỐI B ALL DONE — $(date +%H:%M:%S) ############"
sed -n '/Bat\/1000/p' braincompare_v13.log
