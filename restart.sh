cd ~/project-bot/
git pull
screen -S project-bot -X quit
gradlew shadowJar
cp ./build/libs/*.jar ./bot.jar
screen -dm -S project-bot start.sh
