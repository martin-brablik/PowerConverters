package covers1624.powerconverters.tile.ic2;

import covers1624.powerconverters.handler.ConfigurationHandler;
import covers1624.powerconverters.init.PowerSystems;
import covers1624.powerconverters.tile.main.TileEntityEnergyConsumer;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityIndustrialCraftConsumer extends TileEntityEnergyConsumer<IEnergySink> implements IEnergySink {
    private boolean isAddedToEnergyNet;
    private boolean didFirstAddToNet;
    private double euLastTick;
    private long lastTickInjected;

    public TileEntityIndustrialCraftConsumer(int voltageIndex) {
        super(PowerSystems.powerSystemIndustrialCraft, voltageIndex, IEnergySink.class);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!didFirstAddToNet && !worldObj.isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            didFirstAddToNet = true;
            isAddedToEnergyNet = true;
        }

        if (worldObj.getWorldTime() - lastTickInjected > 2) {
            euLastTick = 0;
        }
    }

    @Override
    public void onChunkUnload() {
        MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
    }

    @Override
    public void validate() {
        super.validate();
        if (!isAddedToEnergyNet) {
            didFirstAddToNet = false;
        }
    }

    @Override
    public void invalidate() {
        if (isAddedToEnergyNet) {
            if (!worldObj.isRemote) {
                MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            }
            isAddedToEnergyNet = false;
        }
        super.invalidate();
    }

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
        return !ConfigurationHandler.disableIC2Consumer;
    }

    @Override
    public double getDemandedEnergy() {
        return getTotalEnergyDemand() / PowerSystems.powerSystemIndustrialCraft.getScaleAmmount();
    }

    @Override
    public double injectEnergy(ForgeDirection directionFrom, double realAmount, double voltage) {
        // Overvoltage: a source of a higher tier (LV < MV < HV < EV) than this consumer blows it up.
        // The packet size is compared against the max input of this consumer's tier, not against the tier number.
        if (realAmount > getMaxInputPacket()) {
            if (!worldObj.isRemote) {
                worldObj.setBlockToAir(xCoord, yCoord, zCoord);
                worldObj.createExplosion(null, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, 1.0F + getSinkTier(), true);
            }
            return 0;
        }

        double pcuNotStored = storeEnergy(realAmount * PowerSystems.powerSystemIndustrialCraft.getScaleAmmount(), false);
        double euNotStored = pcuNotStored / PowerSystems.powerSystemIndustrialCraft.getScaleAmmount();

        double euThisInjection = (realAmount - euNotStored);

        if (lastTickInjected == worldObj.getWorldTime()) {
            euLastTick += euThisInjection;
        } else {
            euLastTick = euThisInjection;
            lastTickInjected = worldObj.getWorldTime();
        }

        return euNotStored;
    }

    /**
     * Largest EU packet this consumer accepts without exploding, based on its tier (LV 32, MV 128, HV 512, EV 2048).
     */
    private double getMaxInputPacket() {
        return PowerSystems.powerSystemIndustrialCraft.getVoltageValues()[getVoltageIndex()];
    }

    @Override
    public int getSinkTier() {
        return getVoltageIndex() + 1;
    }

    @Override
    public double getInputRate() {
        return euLastTick;
    }
}
