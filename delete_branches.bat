@echo off
REM Delete the topic branch from the main worktree
cd /d D:\document\openSource\TS6_Droid_CN

echo === Deleting branch agents/gradle-build-failure-debugging ===
git branch -D agents/gradle-build-failure-debugging

echo === Deleting remote branch ===
git push origin --delete agents/gradle-build-failure-debugging

echo === Current branches ===
git branch -a

echo === Cleanup complete ===
