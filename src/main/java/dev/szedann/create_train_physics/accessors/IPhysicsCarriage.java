package dev.szedann.create_train_physics.accessors;

import org.jetbrains.annotations.Nullable;

public interface IPhysicsCarriage {
    @Nullable Integer railways$getMass();
    void railways$setMass(int mass);
    @Nullable Integer trainphys$getEngineCount();
    void trainphys$setEngineCount(int engineCount);
}