package net.smmeyers.stagedspawnsaggro;

public interface PlayerMixinAccessor {
    int getAggressionDifficulty();
    void setAggressionDifficulty(int aggressionDifficulty);

    int getSpawnDifficulty();
    void setSpawnDifficulty(int spawnDifficulty);

    int getDaysPlayed();
    void setDaysPlayed(int daysPlayed);
}
