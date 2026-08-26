#!/usr/bin/env bash
# P3 — đóng lưới sạch: MC @ state THÔ (1-bit, -DcoarseMC) NHƯNG reward+gamma = rl_struct.
# Khử confound reward của ô MC@thô-v8 (vốn dùng reward cũ). So với MC@mịn (v10) dưới CÙNG reward
# -> biết MC thực sự thích thô hay mịn. Eval: -DcoarseMC bật => MC thô; QL KHÔNG bật coarseQL => QL mịn.
set -uo pipefail
cd "$(dirname "$0")"

SNAP=/tmp/p3_classes
rm -rf "$SNAP" && cp -r target/classes "$SNAP"
CP="$SNAP"
for j in $(find "$HOME/.m2/repository/org/openjfx" -name '*.jar' 2>/dev/null | grep -vE 'sources|javadoc'); do CP="$CP:$j"; done

MC=org.openjfx.app.core.montecarlo.MonteCarloTrainer
BC=org.openjfx.app.core.BrainCompare
mkdir -p qtables_backup

echo "############ P3 MC@coarse + reward rl_struct — $(date +%H:%M:%S) ############"
cp -f qtables/mc_wolf.qtable "qtables_backup/mc_wolf_fine3000_$(date +%H%M%S).qtable" 2>/dev/null || true
rm -f qtables/mc_wolf.qtable
echo ">> TRAIN MC sói: -DcoarseMC=true (state 1-bit), reward+gamma rl_struct, 800 ep x 800 step (từ rỗng) $(date +%H:%M:%S)"
java -DcoarseMC=true -cp "$CP" $MC 800 800 wolf 2>&1 | tee train_mc_coarse.log
echo ">> EVAL v12 (-DcoarseMC: MC=thô, QL=mịn committed, RBS) $(date +%H:%M:%S)"
java -DcoarseMC=true -cp "$CP" $BC 50 3000 2>&1 | tee braincompare_v12.log
cp -f braincompare_perseed.csv braincompare_perseed_v12.csv
python3 stats_rliable.py braincompare_perseed.csv catches_per_1000 10000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v12.txt

echo "############ P3 ALL DONE — $(date +%H:%M:%S) ############"
sed -n '/Bat\/1000/p' braincompare_v12.log
