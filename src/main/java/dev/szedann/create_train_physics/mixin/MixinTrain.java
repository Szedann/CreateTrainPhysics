package dev.szedann.create_train_physics.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.szedann.create_train_physics.Config;
import dev.szedann.create_train_physics.accessors.IPhysicsCarriage;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = Train.class, remap = false)
public abstract class MixinTrain {
    @Shadow
    public boolean derailed;

    @Shadow
    public List<Carriage> carriages;

    @Shadow
    public TrackGraph graph;

    @Shadow
    public double speed;

    @Shadow
    public double targetSpeed;

    @Shadow
    public abstract void leaveStation();

    @Shadow
    public boolean manualTick;

    @Shadow
    public int fuelTicks;

    @Shadow
    public abstract float maxSpeed();

    @Shadow
    public UUID currentStation;

    @Shadow
    public UUID id;

    @Shadow
    public abstract float maxTurnSpeed();

    @Shadow
    public abstract void crash();

    @Unique private double railways$rollingResistanceCoefficient(){ return 0.001; }
    @Unique private double railways$frictionCoefficient(){
        return 0.4;
//        return carriages.getFirst().leadingBogey().leading().edge.getTrackMaterial().trackType
//                == CRTrackMaterials.CRTrackType.MONORAIL
//                ? 0.8
//                : 0.4;
    }
    @Unique private double railways$powerUsage = 0;
    //    @Unique private boolean railways$isRaining = false;
    @Unique private double railways$energyUsed = 0;

    @WrapMethod(method = "collideWithOtherTrains")
    public void collideWithOtherTrains(Level level, Carriage carriage, Operation<Void> original) {
        if (derailed)
            return;

        TravellingPoint trailingPoint = carriage.getTrailingPoint();
        TravellingPoint leadingPoint = carriage.getLeadingPoint();


        if (leadingPoint.node1 == null || trailingPoint.node1 == null)
            return;
        ResourceKey<Level> dimension = leadingPoint.node1.getLocation().dimension;
        if (!dimension.equals(trailingPoint.node1.getLocation().dimension))
            return;

        Vec3 start = (speed < 0 ? trailingPoint : leadingPoint).getPosition(graph);
        Vec3 end = (speed < 0 ? leadingPoint : trailingPoint).getPosition(graph);

        Pair<Carriage, Vec3> collision = railways$findCollidingCarriage(level, start, end, dimension);
        if (collision == null)
            return;

        Train train = collision.getFirst().train;

        Carriage otherCarriage = collision.getFirst();

        double yawDiff = Math.abs(railways$getCarriageYaw(carriage)-railways$getCarriageYaw(otherCarriage));

        int directionMultiplier = (yawDiff > Math.PI/2 && yawDiff < Math.PI*1.5) ? -1 : 1;

        double relativeSpeed = Math.abs(directionMultiplier * speed + train.speed);

        if (relativeSpeed > 6 || (yawDiff%Math.PI>Math.PI/4 && yawDiff%Math.PI<Math.PI*0.75)) {
            Vec3 v = collision.getSecond();
            level.explode(null, v.x, v.y, v.z, (float) Math.min(3 * relativeSpeed, 5), Level.ExplosionInteraction.NONE);
            crash();
            train.crash();
            return;
        }

        int m1 = railways$getMass();
        int m2 = train.carriages.stream().mapToInt(this::railways$getCarriageMass).sum();

        double u1 = directionMultiplier * speed * 20;
        double u2 = train.speed * 20;

        // Coefficient of restitution
        double e = 0.5;

        double v1 = (m1*u1 + m2*u2 + m2*e*(u2-u1)) / (m1+m2);
        double v2 = (m1*u1 + m2*u2 + m1*e*(u1-u2)) / (m1+m2);

        speed = directionMultiplier * v1 / 20;
        train.speed = v2 / 20;


    }

    @Inject(method = "tickPassiveSlowdown", at=@At("HEAD"), cancellable = true)
    public void tickPassiveSlowdown(CallbackInfo ci) {
        ci.cancel();
        if(currentStation != null) return;
        double gravityAcceleration = railways$getGravityAcceleration();
        double aerodynamicAcceleration = -railways$forceToAcceleration(railways$getAerodynamicDrag());
        double rollingFrictionAcceleration = -railways$forceToAcceleration(railways$getRollingFriction())
                * Math.signum(speed);
//        Railways.LOGGER.info("gravity {}, aerodynamic {}, rollingFriction {}",
//                String.format("%.2f", gravityAcceleration*20),
//                String.format("%.2f", aerodynamicAcceleration*20),
//                String.format("%.2f", rollingFrictionAcceleration*20));

        speed += gravityAcceleration + aerodynamicAcceleration + rollingFrictionAcceleration;
//        carriages.forEach(carriage -> carriage.bogeys.stream().filter(Objects::nonNull).forEach(carriageBogey -> railways$applyWheelSlip(carriageBogey,.1)));
    }

    @Unique
    private int railways$getCarriageMass(Carriage carriage){
        Integer carriageMass = ((IPhysicsCarriage) carriage).railways$getMass();
        if(carriageMass == null) carriageMass = 1;
        AtomicInteger cargoMass = new AtomicInteger();
//        CombinedInvWrapper storageItems =  carriage.storage.getAllItems();
//        if(storageItems != null)
//            storageItems.(storage-> cargoMass.addAndGet((int) (storage.getAmount() * 10)));
        return carriageMass * 500 + cargoMass.get();
    }

    @Unique
    private int railways$getMass(){
        int mass = 0;
        for(Carriage carriage : carriages) mass += railways$getCarriageMass(carriage);
        return mass;
    }

    @Unique
    private int railways$getCarriagePower(Carriage carriage){
//        count the engines and sth
        Integer engineCount = ((IPhysicsCarriage)carriage).trainphys$getEngineCount();
        if(engineCount == null) engineCount = 0;
        return engineCount * Config.enginePower * 1000;
    }

    @Unique
    private int railways$getPower(){
        return carriages.stream().mapToInt(this::railways$getCarriagePower).sum(); // force in W
    }

    /**
     * @see <a href="https://en.wikipedia.org/wiki/Adhesion_railway#Effect_of_adhesion_limits">Effect of adhesion limits</a>
     */
    @Unique
    private double railways$getMaxTractiveEffort(){
        double friction = railways$frictionCoefficient();
        return carriages.stream().mapToDouble(carriage -> friction*railways$getCarriageMass(carriage)*9.81).sum();
    }


    @Unique
    private double railways$getMaxSpeed(){
        double p = railways$getMass()*railways$rollingResistanceCoefficient()/railways$getDragConstant();
        double q = -railways$getPower()/railways$getDragConstant();
        double D = q*q + Math.pow(p*2/3,3);
        double DRoot = Math.sqrt(D);
        return Math.min(Math.cbrt(-q+DRoot)+Math.cbrt(-q-DRoot), AllConfigs.server().trains.trainTopSpeed.getF()) / 20;
    }

    @Inject(method = "maxSpeed", at = @At("RETURN"), cancellable = true)
    public void maxSpeed(CallbackInfoReturnable<Float> cir) {
            cir.setReturnValue((float) railways$getMaxSpeed());

    }

    @Inject(method = "maxTurnSpeed", at = @At("RETURN"), cancellable = true)
    public void maxTurnSpeed(CallbackInfoReturnable<Float> cir) {



        BezierConnection turn = carriages.getFirst().getTrailingPoint().edge.getTurn();
        if(turn == null) turn = carriages.getLast().getLeadingPoint().edge.getTurn();
        if(turn == null) {
            cir.setReturnValue(maxSpeed());
            return;
        }

        double r = turn.getRadius();
        // this tries to approximate a radius for s-bends
        if(r == 0){
            Vec3 p1 = turn.getPosition(0);
            Vec3 p2 = turn.getPosition(turn.getLength());
            int x = (int) Math.abs(p1.x-p2.x)/2;
            int z = (int) Math.abs(p1.z-p2.z)/2;
            int shorterSide = Math.min(x, z);
            r = (double) (x * x + z * z) / (2*shorterSide);
        }
        float speed = (float) (Math.sqrt(9.81 * r * (railways$frictionCoefficient()/(1-railways$frictionCoefficient())))/20);

        cir.setReturnValue(Math.min(maxSpeed(), speed));

    }

    @Inject(method = "acceleration", at = @At("HEAD"), cancellable = true)
    public void acceleration(CallbackInfoReturnable<Float> cir) {
        if (Config.requireFuel && fuelTicks <= 0)
            cir.setReturnValue(AllConfigs.server().trains.trainAcceleration.getF() / (400 * 20));
    }

    @Inject(method = "approachTargetSpeed", at = @At("HEAD"), cancellable = true)
    public void approachTargetSpeed(float accelerationMod, CallbackInfo ci) {
        ci.cancel();

        double actualTarget = targetSpeed;
        if (Mth.equal(actualTarget, speed))
            return;
        if (manualTick)
            leaveStation();
        if(speed == actualTarget) return;
        double velocity = Math.abs(speed*20); // velocity in m/s
        double force;
        double maxPower = Math.abs(actualTarget-speed)*railways$getMass()*(20*20);
        if(Math.abs(targetSpeed) > Math.abs(speed) && targetSpeed * speed>0)
        {
            force = Math.min(Math.min(railways$getPower()/velocity, railways$getMaxTractiveEffort()), maxPower);
            railways$powerUsage = force * velocity;
        }else{
            force = Math.min(railways$getMaxTractiveEffort(), maxPower);
            railways$powerUsage = 0;
        }
        double acceleration = railways$forceToAcceleration(force);
        if (Config.requireFuel && fuelTicks <= 0)
            acceleration = AllConfigs.server().trains.trainAcceleration.getF() / (400 * 20);
        if (speed < actualTarget)
            speed = Math.min(speed + acceleration, actualTarget);
        else if (speed > actualTarget)
            speed = Math.max(speed - acceleration, actualTarget);
    }

    @Unique private double railways$getGravityAcceleration(){
        if(derailed) return  0;
        Vec3 leading = carriages.getFirst().getLeadingPoint().getPosition(graph);
        Vec3 trailing = carriages.getLast().getTrailingPoint().getPosition(graph);
        double horizontalDistance = Math.sqrt(Math.pow(leading.x - trailing.x, 2) + Math.pow(leading.z - trailing.z, 2));
        double verticalDistance = leading.y - trailing.y;
        double incline = Math.atan2(verticalDistance, horizontalDistance); // (-0.5pi,0): decline, (0, 0.5pi): incline 0: no incline
        double gravityPerTick = -9.81 / (20 * 20); // 1s = 20t
        double gravityMultiplier = 1;
        return gravityMultiplier * gravityPerTick * Math.sin(incline);
    }

    @Unique private double railways$getDragConstant(){
        double airDensity = 1.204;
        double dragCoefficient = 0.35;
        double area = 9;
        return airDensity * dragCoefficient * area;
    }

    @Unique private double railways$getAerodynamicDrag(){
        double velocity = speed * 20;
        return railways$getDragConstant() * 0.5 * Math.pow(velocity, 2) * (speed > 0 ? 1 : -1);
    }

    @Unique private double railways$getRollingFriction(){
        return (railways$rollingResistanceCoefficient() * (railways$getMass()*9.81));
    }

//    @Unique private void railways$applyWheelSlip(CarriageBogey bogey, double distance){
//        CRPackets.PACKETS.sendTo(PlayerSelection.all(), new WheelslipPacket(bogey, distance));
//    }

    @Unique double railways$forceToAcceleration(double force){
        return force / railways$getMass() / (20*20);
    }

    @Inject(method = "tick", at=@At("TAIL"))
    public void tick(CallbackInfo ci) {
        railways$energyUsed += railways$powerUsage / 20;
        double vmax = maxTurnSpeed();
        carriages.forEach(c->c.forEachPresentEntity(cce->cce.getPassengers().forEach(p->{
            if(!(p instanceof Player player)) return;
            player.displayClientMessage(Component.literal(String.format("%.0fW P - %.0fb/s v - %.0fb/s vmax - %dkg mass",
                    railways$powerUsage, speed*20, vmax*20, railways$getMass())), true);
        })));
    }

    @Inject(method = "burnFuel", at=@At("HEAD"), cancellable = true)
    public void burnFuel(CallbackInfo ci) {
        if(fuelTicks <= 0) return;
        int joulesPerTick = 15000; // rough estimate based on coal
        int ticks = (int) railways$energyUsed / joulesPerTick;
        railways$energyUsed %= joulesPerTick;

        fuelTicks -= ticks;
        ci.cancel();
    }

    @Unique
    public double railways$getCarriageYaw(Carriage carriage) {
        Vec3 diff = carriage.getLeadingPoint().getPosition(carriage.train.graph)
                .subtract(carriage.getTrailingPoint().getPosition(carriage.train.graph)).normalize();
        return Math.atan2(diff.x, diff.z);
    }

    @Unique
    public Pair<Carriage, Vec3> railways$findCollidingCarriage(Level level, Vec3 start, Vec3 end, ResourceKey<Level> dimension) {
        Vec3 diff = end.subtract(start);
        double maxDistanceSqr = Math.pow(AllConfigs.server().trains.maxAssemblyLength.get(), 2.0);

        Trains: for (Train train : Create.RAILWAYS.sided(level).trains.values()) {
            if (train.id == this.id)
                continue;
            if (train.graph != null && train.graph != graph)
                continue;

            Vec3 lastPoint = null;

            for (Carriage otherCarriage : train.carriages) {
                for (boolean betweenBits : Iterate.trueAndFalse) {
                    if (betweenBits && lastPoint == null)
                        continue;

                    TravellingPoint otherLeading = otherCarriage.getLeadingPoint();
                    TravellingPoint otherTrailing = otherCarriage.getTrailingPoint();
                    if (otherLeading.edge == null || otherTrailing.edge == null)
                        continue;
                    ResourceKey<Level> otherDimension = otherLeading.node1.getLocation().dimension;
                    if (!otherDimension.equals(otherTrailing.node1.getLocation().dimension))
                        continue;
                    if (!otherDimension.equals(dimension))
                        continue;

                    Vec3 start2 = otherLeading.getPosition(train.graph);
                    Vec3 end2 = otherTrailing.getPosition(train.graph);

                    if (Math.min(start2.distanceToSqr(start), end2.distanceToSqr(start)) > maxDistanceSqr)
                        continue Trains;

                    if (betweenBits) {
                        end2 = start2;
                        start2 = lastPoint;
                    }

                    lastPoint = end2;

                    if ((end.y < end2.y - 3 || end2.y < end.y - 3)
                            && (start.y < start2.y - 3 || start2.y < start.y - 3))
                        continue;

                    Vec3 diff2 = end2.subtract(start2);
                    Vec3 normedDiff = diff.normalize();
                    Vec3 normedDiff2 = diff2.normalize();
                    double[] intersect = VecHelper.intersect(start, start2, normedDiff, normedDiff2, Direction.Axis.Y);

                    if (intersect == null) {
                        Vec3 intersectSphere = VecHelper.intersectSphere(start2, normedDiff2, start, .125f);
                        if (intersectSphere == null)
                            continue;
                        if (!Mth.equal(normedDiff2.dot(intersectSphere.subtract(start2)
                                .normalize()), 1))
                            continue;
                        intersect = new double[2];
                        intersect[0] = intersectSphere.distanceTo(start) - .125;
                        intersect[1] = intersectSphere.distanceTo(start2) - .125;
                    }

                    if (intersect[0] > diff.length())
                        continue;
                    if (intersect[1] > diff2.length())
                        continue;
                    if (intersect[0] < 0)
                        continue;
                    if (intersect[1] < 0)
                        continue;


                    return Pair.of(otherCarriage, start.add(normedDiff.scale(intersect[0])));
                }
            }
        }
        return null;
    }
}
