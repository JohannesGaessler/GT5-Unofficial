package gregtech.api.metatileentity.implementations;

import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Dyes;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.MetaPipeEntity;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.objects.XSTR;
import gregtech.api.util.GT_Log;
import gregtech.api.util.GT_Utility;
import gregtech.common.GT_Client;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static gregtech.api.enums.GT_Values.D1;

public class GT_MetaPipeEntity_Fluid extends MetaPipeEntity {
    public final float mThickNess;
    public final Materials mMaterial;
    public final int mCapacity, mHeatResistance;
    public final boolean mGasProof;
    public FluidStack mFluid;
    public byte mLastReceivedFrom = 0, oLastReceivedFrom = 0;

    public GT_MetaPipeEntity_Fluid(int aID, String aName, String aNameRegional, float aThickNess, Materials aMaterial, int aCapacity, int aHeatResistance, boolean aGasProof) {
        super(aID, aName, aNameRegional, 0);
        mThickNess = aThickNess;
        mMaterial = aMaterial;
        mCapacity = aCapacity;
        mGasProof = aGasProof;
        mHeatResistance = aHeatResistance;
    }

    public GT_MetaPipeEntity_Fluid(String aName, float aThickNess, Materials aMaterial, int aCapacity, int aHeatResistance, boolean aGasProof) {
        super(aName, 0);
        mThickNess = aThickNess;
        mMaterial = aMaterial;
        mCapacity = aCapacity;
        mGasProof = aGasProof;
        mHeatResistance = aHeatResistance;
    }

    @Override
    public byte getTileEntityBaseType() {
        return mMaterial == null ? 4 : (byte) ((mMaterial.contains(SubTag.WOOD) ? 12 : 4) + Math.max(0, Math.min(3, mMaterial.mToolQuality)));
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaPipeEntity_Fluid(mName, mThickNess, mMaterial, mCapacity, mHeatResistance, mGasProof);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aConnections, byte aColorIndex, boolean aConnected, boolean aRedstone) {
        if (aConnected) {
            float tThickNess = getThickNess();
            if (tThickNess < 0.124F)
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[OrePrefixes.pipe.mTextureIndex], Dyes.getModulation(aColorIndex, mMaterial.mRGBa))};
            if (tThickNess < 0.374F)//0.375
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[OrePrefixes.pipeTiny.mTextureIndex], Dyes.getModulation(aColorIndex, mMaterial.mRGBa))};
            if (tThickNess < 0.499F)//0.500
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[OrePrefixes.pipeSmall.mTextureIndex], Dyes.getModulation(aColorIndex, mMaterial.mRGBa))};
            if (tThickNess < 0.749F)//0.750
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[OrePrefixes.pipeMedium.mTextureIndex], Dyes.getModulation(aColorIndex, mMaterial.mRGBa))};
            if (tThickNess < 0.874F)//0.825
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[OrePrefixes.pipeLarge.mTextureIndex], Dyes.getModulation(aColorIndex, mMaterial.mRGBa))};
            return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[OrePrefixes.pipeHuge.mTextureIndex], Dyes.getModulation(aColorIndex, mMaterial.mRGBa))};
        }
        return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[OrePrefixes.pipe.mTextureIndex], Dyes.getModulation(aColorIndex, mMaterial.mRGBa))};
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return false;
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return false;
    }

    @Override
    public final boolean renderInside(byte aSide) {
        return false;
    }

    @Override
    public int getProgresstime() {
        return getFluidAmount();
    }

    @Override
    public int maxProgresstime() {
        return getCapacity();
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        if (mFluid != null) aNBT.setTag("mFluid", mFluid.writeToNBT(new NBTTagCompound()));
        aNBT.setByte("mLastReceivedFrom", mLastReceivedFrom);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        mFluid = FluidStack.loadFluidStackFromNBT(aNBT.getCompoundTag("mFluid"));
        mLastReceivedFrom = aNBT.getByte("mLastReceivedFrom");
    }

    @Override
    public void onEntityCollidedWithBlock(World aWorld, int aX, int aY, int aZ, Entity aEntity) {
        if (mFluid != null && (((BaseMetaPipeEntity) getBaseMetaTileEntity()).mConnections & -128) == 0 && aEntity instanceof EntityLivingBase) {
            int tTemperature = mFluid.getFluid().getTemperature(mFluid);
            if (tTemperature > 320 && !isCoverOnSide((BaseMetaPipeEntity) getBaseMetaTileEntity(), (EntityLivingBase) aEntity)) {
                GT_Utility.applyHeatDamage((EntityLivingBase) aEntity, (tTemperature - 300) / 50.0F);
            } else if (tTemperature < 260 && !isCoverOnSide((BaseMetaPipeEntity) getBaseMetaTileEntity(), (EntityLivingBase) aEntity)) {
                GT_Utility.applyFrostDamage((EntityLivingBase) aEntity, (270 - tTemperature) / 25.0F);
            }
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World aWorld, int aX, int aY, int aZ) {
    	float tSpace = (1f - getThickNess())/2;
    	float tSide0 = tSpace;
    	float tSide1 = 1f - tSpace;
    	float tSide2 = tSpace;
    	float tSide3 = 1f - tSpace;
    	float tSide4 = tSpace;
    	float tSide5 = 1f - tSpace;
    	
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 0) != 0){tSide0=tSide2=tSide4=0;tSide3=tSide5=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 1) != 0){tSide2=tSide4=0;tSide1=tSide3=tSide5=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 2) != 0){tSide0=tSide2=tSide4=0;tSide1=tSide5=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 3) != 0){tSide0=tSide4=0;tSide1=tSide3=tSide5=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 4) != 0){tSide0=tSide2=tSide4=0;tSide1=tSide3=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 5) != 0){tSide0=tSide2=0;tSide1=tSide3=tSide5=1;}
    	
    	byte tConn = ((BaseMetaPipeEntity) getBaseMetaTileEntity()).mConnections;
    	if((tConn & (1 << ForgeDirection.DOWN.ordinal()) ) != 0) tSide0 = 0f;
    	if((tConn & (1 << ForgeDirection.UP.ordinal())   ) != 0) tSide1 = 1f;
    	if((tConn & (1 << ForgeDirection.NORTH.ordinal())) != 0) tSide2 = 0f;
    	if((tConn & (1 << ForgeDirection.SOUTH.ordinal())) != 0) tSide3 = 1f;
    	if((tConn & (1 << ForgeDirection.WEST.ordinal()) ) != 0) tSide4 = 0f;
    	if((tConn & (1 << ForgeDirection.EAST.ordinal()) ) != 0) tSide5 = 1f;
    	
    	return AxisAlignedBB.getBoundingBox(aX + tSide4, aY + tSide0, aZ + tSide2, aX + tSide5, aY + tSide1, aZ + tSide3);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide() && aTick % 5 == 0) {
            mLastReceivedFrom &= 63;
            if (mLastReceivedFrom == 63) {
                mLastReceivedFrom = 0;
            }

            if (mFluid != null && mFluid.amount > 0) {
                int tTemperature = mFluid.getFluid().getTemperature(mFluid);
                if (tTemperature > mHeatResistance) {
                    if (aBaseMetaTileEntity.getRandomNumber(100) == 0) {
                        aBaseMetaTileEntity.setToFire();
                        return;
                    }
                    aBaseMetaTileEntity.setOnFire();
                }
                if (!mGasProof && mFluid.getFluid().isGaseous(mFluid)) {
                    mFluid.amount -= 5;
                    sendSound((byte) 9);
                    if (tTemperature > 320) {
                        try {
                            for (EntityLivingBase tLiving : (ArrayList<EntityLivingBase>) getBaseMetaTileEntity().getWorld().getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(getBaseMetaTileEntity().getXCoord() - 2, getBaseMetaTileEntity().getYCoord() - 2, getBaseMetaTileEntity().getZCoord() - 2, getBaseMetaTileEntity().getXCoord() + 3, getBaseMetaTileEntity().getYCoord() + 3, getBaseMetaTileEntity().getZCoord() + 3))) {
                                GT_Utility.applyHeatDamage(tLiving, (tTemperature - 300) / 25.0F);
                            }
                        } catch (Throwable e) {
                            if (D1) e.printStackTrace(GT_Log.err);
                        }
                    } else if (tTemperature < 260) {
                        try {
                            for (EntityLivingBase tLiving : (ArrayList<EntityLivingBase>) getBaseMetaTileEntity().getWorld().getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(getBaseMetaTileEntity().getXCoord() - 2, getBaseMetaTileEntity().getYCoord() - 2, getBaseMetaTileEntity().getZCoord() - 2, getBaseMetaTileEntity().getXCoord() + 3, getBaseMetaTileEntity().getYCoord() + 3, getBaseMetaTileEntity().getZCoord() + 3))) {
                                GT_Utility.applyFrostDamage(tLiving, (270 - tTemperature) / 12.5F);
                            }
                        } catch (Throwable e) {
                            if (D1) e.printStackTrace(GT_Log.err);
                        }
                    }
                    if (mFluid.amount <= 0) mFluid = null;
                }
            }

            if (mLastReceivedFrom == oLastReceivedFrom) {
                ConcurrentHashMap<IFluidHandler, ForgeDirection> tTanks = new ConcurrentHashMap<IFluidHandler, ForgeDirection>();

                mConnections = 0;

                for (byte tSide = 0, i = 0, j = (byte) aBaseMetaTileEntity.getRandomNumber(6); i < 6; i++) {
                    tSide = (byte) ((j + i) % 6);

                    IFluidHandler tTileEntity = aBaseMetaTileEntity.getITankContainerAtSide(tSide);
                    if (tTileEntity != null) {
                        if (tTileEntity instanceof IGregTechTileEntity) {
                            if (aBaseMetaTileEntity.getColorization() >= 0) {
                                byte tColor = ((IGregTechTileEntity) tTileEntity).getColorization();
                                if (tColor >= 0 && (tColor & 15) != (aBaseMetaTileEntity.getColorization() & 15)) {
                                    continue;
                                }
                            }
                        }   
                        FluidTankInfo[] tInfo = tTileEntity.getTankInfo(ForgeDirection.getOrientation(tSide).getOpposite());
                        if (tInfo != null && tInfo.length > 0) {
                            if (tTileEntity instanceof ICoverable && ((ICoverable) tTileEntity).getCoverBehaviorAtSide(GT_Utility.getOppositeSide(tSide)).alwaysLookConnected(GT_Utility.getOppositeSide(tSide), ((ICoverable) tTileEntity).getCoverIDAtSide(GT_Utility.getOppositeSide(tSide)), ((ICoverable) tTileEntity).getCoverDataAtSide(GT_Utility.getOppositeSide(tSide)), ((ICoverable) tTileEntity))) {
                                mConnections |= (1 << tSide);
                            }
                            if (aBaseMetaTileEntity.getCoverBehaviorAtSide(tSide).letsFluidIn(tSide, aBaseMetaTileEntity.getCoverIDAtSide(tSide), aBaseMetaTileEntity.getCoverDataAtSide(tSide), null, aBaseMetaTileEntity)) {
                                mConnections |= (1 << tSide);
                            }
                            if (aBaseMetaTileEntity.getCoverBehaviorAtSide(tSide).letsFluidOut(tSide, aBaseMetaTileEntity.getCoverIDAtSide(tSide), aBaseMetaTileEntity.getCoverDataAtSide(tSide), null, aBaseMetaTileEntity)) {
                                mConnections |= (1 << tSide);
                                if (((1 << tSide) & mLastReceivedFrom) == 0)
                                    tTanks.put(tTileEntity, ForgeDirection.getOrientation(tSide).getOpposite());
                            }

                            if (aBaseMetaTileEntity.getCoverBehaviorAtSide(tSide).alwaysLookConnected(tSide, aBaseMetaTileEntity.getCoverIDAtSide(tSide), aBaseMetaTileEntity.getCoverDataAtSide(tSide), aBaseMetaTileEntity)) {
                                mConnections |= (1 << tSide);
                            }
                        }else if(tInfo != null && tInfo.length == 0){  
                        	IGregTechTileEntity tSideTile = aBaseMetaTileEntity.getIGregTechTileEntityAtSide(tSide);
                            if(tSideTile!=null){
                            	ItemStack tCover = tSideTile.getCoverItemAtSide(GT_Utility.getOppositeSide(tSide));
                            	if (tCover!=null &&(GT_Utility.areStacksEqual(tCover, ItemList.FluidRegulator_LV.get(1, new Object[]{},true)) || 
                            			GT_Utility.areStacksEqual(tCover, ItemList.FluidRegulator_MV.get(1, new Object[]{},true)) || 
                            			GT_Utility.areStacksEqual(tCover, ItemList.FluidRegulator_HV.get(1, new Object[]{},true)) || 
                            			GT_Utility.areStacksEqual(tCover, ItemList.FluidRegulator_EV.get(1, new Object[]{},true)) || 
                            			GT_Utility.areStacksEqual(tCover, ItemList.FluidRegulator_IV.get(1, new Object[]{},true)))) {
                            		mConnections |= (1 << tSide);
                            	}
                            }                        	
                        }
                    }
                }

                if (mFluid != null && mFluid.amount > 0) {
                    int tAmount = Math.max(1, Math.min(mCapacity * 10, mFluid.amount / 2)), tSuccessfulTankAmount = 0;

                    for (Entry<IFluidHandler, ForgeDirection> tEntry : tTanks.entrySet())
                        if (tEntry.getKey().fill(tEntry.getValue(), drain(tAmount, false), false) > 0)
                            tSuccessfulTankAmount++;

                    if (tSuccessfulTankAmount > 0) {
                        if (tAmount >= tSuccessfulTankAmount) {
                            tAmount /= tSuccessfulTankAmount;
                            for (Entry<IFluidHandler, ForgeDirection> tTileEntity : tTanks.entrySet()) {
                                if (mFluid == null || mFluid.amount <= 0) break;
                                int tFilledAmount = tTileEntity.getKey().fill(tTileEntity.getValue(), drain(tAmount, false), false);
                                if (tFilledAmount > 0)
                                    tTileEntity.getKey().fill(tTileEntity.getValue(), drain(tFilledAmount, true), true);
                            }
                        } else {
                            for (Entry<IFluidHandler, ForgeDirection> tTileEntity : tTanks.entrySet()) {
                                if (mFluid == null || mFluid.amount <= 0) break;
                                int tFilledAmount = tTileEntity.getKey().fill(tTileEntity.getValue(), drain(mFluid.amount, false), false);
                                if (tFilledAmount > 0)
                                    tTileEntity.getKey().fill(tTileEntity.getValue(), drain(tFilledAmount, true), true);
                            }
                        }
                    }
                }

                mLastReceivedFrom = 0;
            }

            oLastReceivedFrom = mLastReceivedFrom;
        }else if(aBaseMetaTileEntity.isClientSide() && GT_Client.changeDetected==4) aBaseMetaTileEntity.issueTextureUpdate();
    }

    @Override
    public void doSound(byte aIndex, double aX, double aY, double aZ) {
        super.doSound(aIndex, aX, aY, aZ);
        if (aIndex == 9) {
            GT_Utility.doSoundAtClient(GregTech_API.sSoundList.get(4), 5, 1.0F, aX, aY, aZ);
            for (byte i = 0; i < 6; i++)
                for (int l = 0; l < 2; ++l)
                    getBaseMetaTileEntity().getWorld().spawnParticle("largesmoke", aX - 0.5 + (new XSTR()).nextFloat(), aY - 0.5 + (new XSTR()).nextFloat(), aZ - 0.5 + (new XSTR()).nextFloat(), ForgeDirection.getOrientation(i).offsetX / 5.0, ForgeDirection.getOrientation(i).offsetY / 5.0, ForgeDirection.getOrientation(i).offsetZ / 5.0);
        }
    }

    @Override
    public final int getCapacity() {
        return mCapacity * 20;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public final FluidStack getFluid() {
        return mFluid;
    }

    @Override
    public final int getFluidAmount() {
        return mFluid != null ? mFluid.amount : 0;
    }

    @Override
    public final int fill_default(ForgeDirection aSide, FluidStack aFluid, boolean doFill) {
        if (aFluid == null || aFluid.getFluid().getID() <= 0) return 0;

        if (mFluid == null || mFluid.getFluid().getID() <= 0) {
            if (aFluid.amount <= getCapacity()) {
                if (doFill) {
                    mFluid = aFluid.copy();
                    mLastReceivedFrom |= (1 << aSide.ordinal());
                }
                return aFluid.amount;
            }
            if (doFill) {
                mFluid = aFluid.copy();
                mLastReceivedFrom |= (1 << aSide.ordinal());
                mFluid.amount = getCapacity();
            }
            return getCapacity();
        }

        if (!mFluid.isFluidEqual(aFluid)) return 0;

        int space = getCapacity() - mFluid.amount;
        if (aFluid.amount <= space) {
            if (doFill) {
                mFluid.amount += aFluid.amount;
                mLastReceivedFrom |= (1 << aSide.ordinal());
            }
            return aFluid.amount;
        }
        if (doFill) {
            mFluid.amount = getCapacity();
            mLastReceivedFrom |= (1 << aSide.ordinal());
        }
        return space;
    }

    @Override
    public final FluidStack drain(int maxDrain, boolean doDrain) {
        if (mFluid == null) return null;
        if (mFluid.amount <= 0) {
            mFluid = null;
            return null;
        }

        int used = maxDrain;
        if (mFluid.amount < used)
            used = mFluid.amount;

        if (doDrain) {
            mFluid.amount -= used;
        }

        FluidStack drained = mFluid.copy();
        drained.amount = used;

        if (mFluid.amount <= 0) {
            mFluid = null;
        }

        return drained;
    }

    @Override
    public int getTankPressure() {
        return (mFluid == null ? 0 : mFluid.amount) - (getCapacity() / 2);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                EnumChatFormatting.BLUE + "Fluid Capacity: %%%" + (mCapacity * 20) + "L/sec" + EnumChatFormatting.GRAY,
                EnumChatFormatting.RED + "Heat Limit: %%%" + mHeatResistance + " K" + EnumChatFormatting.GRAY
        };
    }

    @Override
    public float getThickNess() {
        if(GT_Mod.instance.isClientSide() && GT_Client.hideValue==1) return 0.0625F;
        return mThickNess;
    }
}