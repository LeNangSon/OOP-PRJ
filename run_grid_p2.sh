#!/usr/bin/env bash
# P2 — LẤP ĐỦ LƯỚI 2x2 (algorithm × state granularity), reward+gamma CỐ ĐỊNH ở rl_struct.
#   P2.1: MC @ state mịn × 3000 ep  -> tách under-training vs bản chất (ô đang tranh cãi).
#   P2.2: QL @ state THÔ (-Dcoarse) × 800 ep -> đo đóng góp riêng của "state mịn" vào lợi thế QL.
# Chạy classpath snapshot để IDE auto-build không wipe target/classes giữa chừng.
set -uo pipefail
cd "$(dirname "$0")"

SNAP=/tmp/gridp2_classes
rm -rf "$SNAP" && cp -r target/classes "$SNAP"
CP="$SNAP"
for j in $(find "$HOME/.m2/repository/org/openjfx" -name '*.jar' 2>/dev/null | grep -vE 'sources|javadoc'); do CP="$CP:$j"; done

QL=org.openjfx.app.core.qlearning.QLearningTrainer
MC=org.openjfx.app.core.montecarlo.MonteCarloTrainer
BC=org.openjfx.app.core.BrainCompare
mkdir -p qtables_backup

# ============================ P2.1 — MC @ fine × 3000 ep ============================
echo "############ P2.1 MC@fine x3000ep — $(date +%H:%M:%S) ############"
cp -f qtables/mc_wolf.qtable "qtables_backup/mc_wolf_fine800_$(date +%H%M%S).qtable" 2>/dev/null || true
rm -f qtables/mc_wolf.qtable
echo ">> TRAIN MC sói: gamma 0.99, 3000 ep x 800 step (từ rỗng) $(date +%H:%M:%S)"
java -cp "$CP" $MC 3000 800 wolf 2>&1 | tee train_mc_fine3000.log
echo ">> EVAL v10 (QL=fine committed, MC=fine3000, RBS) $(date +%H:%M:%S)"
java -cp "$CP" $BC 50 3000 2>&1 | tee braincompare_v10.log
cp -f braincompare_perseed.csv braincompare_perseed_v10.csv
python3 stats_rliable.py braincompare_perseed.csv catches_per_1000 10000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v10.txt

# ============================ P2.2 — QL @ coarse × 800 ep ============================
echo "############ P2.2 QL@coarse(-Dcoarse) x800ep — $(date +%H:%M:%S) ############"
cp -f qtables/wolf.qtable "qtables_backup/wolf_fine_$(date +%H%M%S).qtable" 2>/dev/null || true
rm -f qtables/wolf.qtable
echo ">> TRAIN QL sói: -Dcoarse=true (state 1-bit), reward+gamma vẫn rl_struct, 800 ep x 800 step $(date +%H:%M:%S)"
java -Dcoarse=true -cp "$CP" $QL 800 800 wolf 2>&1 | tee train_ql_coarse.log
echo ">> EVAL v11 (-Dcoarse: QL=coarse, MC=fine3000 [bỏ qua toggle], RBS) $(date +%H:%M:%S)"
java -Dcoarse=true -cp "$CP" $BC 50 3000 2>&1 | tee braincompare_v11.log
cp -f braincompare_perseed.csv braincompare_perseed_v11.csv
python3 stats_rliable.py braincompare_perseed.csv catches_per_1000 10000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v11.txt

echo "############ GRID P2 ALL DONE — $(date +%H:%M:%S) ############"
echo "== v10 (MC@fine3000) =="; sed -n '/Bat\/1000/p' braincompare_v10.log
echo "== v11 (QL@coarse)   =="; sed -n '/Bat\/1000/p' braincompare_v11.log
