package com.gendeathrow.pmobs.entity.New;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIBreakDoor;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.gendeathrow.pmobs.client.LayerFeatures;
import com.gendeathrow.pmobs.client.RaidersSkinManager;
import com.gendeathrow.pmobs.core.PMSettings;
import com.gendeathrow.pmobs.core.RaidersCore;
import com.gendeathrow.pmobs.entity.ai.EntityAIPyromaniac;
import com.gendeathrow.pmobs.entity.ai.EntityAIStealFarmland;
import com.gendeathrow.pmobs.entity.ai.EntityAIStealItemInv;
import com.gendeathrow.pmobs.entity.ai.TwitchersAttack;
import com.gendeathrow.pmobs.handlers.DifficultyProgression;
import com.gendeathrow.pmobs.handlers.EquipmentManager;
import com.gendeathrow.pmobs.handlers.RaiderManager;
import com.gendeathrow.pmobs.handlers.random.ArmorSetWeigthedItem;
import com.google.common.base.Predicate;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;


public class EntityRaiderBase extends EntityMob 
{

	private NetworkPlayerInfo playerInfo;
	
	private ResourceLocation SKIN = null; 
	
	private static PlayerProfileCache profileCache;
	private static MinecraftSessionService sessionService;
	
	private static ArrayList<GameProfile> profileAskedfor;
	
	  
	public static final UUID BABY_SPEED_BOOST_ID = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836");
    public static final UUID SPEED_BOOST_ID = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D837");
    public static final UUID NIGHT_SPEED_BOOST_ID = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D838");
    public static final UUID SPEED_OFFSET_ID = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D839");
    
	private static final DataParameter<String> SKIN_VARIANT = EntityDataManager.<String>createKey(EntityRaiderBase.class, DataSerializers.STRING);
	private static final DataParameter<Boolean> IS_CHILD = EntityDataManager.<Boolean>createKey(EntityRaiderBase.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> RAIDER_VARIANT = EntityDataManager.<Integer>createKey(EntityRaiderBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> RAIDER_ROLE = EntityDataManager.<Integer>createKey(EntityRaiderBase.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> ARMS_RAISED = EntityDataManager.<Boolean>createKey(EntityRaiderBase.class, DataSerializers.BOOLEAN);
    
	private static final AttributeModifier BABY_SPEED_BOOST = new AttributeModifier(BABY_SPEED_BOOST_ID, "Baby speed boost", 0.5D, 1);
	private static final AttributeModifier NIGHT_TIME_BOOST = new AttributeModifier(NIGHT_SPEED_BOOST_ID, "Night speed boost", 0.05D, 1);
	
	private float raiderWidth = -1.0F;
	private float raiderHeight;
	// AI Additions
	private boolean isBreakDoorsTaskSet = false;
    private final EntityAIBreakDoor breakDoor = new EntityAIBreakDoor(this);
 
    private boolean isLeapAttackTaskSet = false;
    private final EntityAILeapAtTarget leapAttack = new EntityAILeapAtTarget(this, 0.4F);

    private boolean isPyroTaskSet = false;
    private final EntityAIPyromaniac pyromaniac = new EntityAIPyromaniac(this, 2D);

    private boolean isTweaker = false;
    private final EntityAIAttackMelee melee = new EntityAIAttackMelee(this, 0.8, false);
    private final TwitchersAttack meleeTweaker = new TwitchersAttack(this, 1, false);
    
    private String playerName;
    
	private GameProfile playerProfile;
	
	private boolean skinErrored = false;
	private long skinTimeOut = 0;
	
	public boolean willSteal = false;
	
	//public LayerFeatures features = LayerFeatures.NONE;
	
	protected DifficultyProgression difficultyManager;
	
    private final InventoryBasic raidersInventory;
	
	public EntityRaiderBase(World worldIn) 
	{
		super(worldIn);
		
		this.raidersInventory = new InventoryBasic("Items", false, 3);
		 
		this.playerName = "Steve";
	
		this.setSize(0.6F, 1.95F);
		
		difficultyManager = new DifficultyProgression(this);

	}
	
	protected void initEntityAI()
	{
	        this.tasks.addTask(0, new EntityAISwimming(this));

	        this.tasks.addTask(4, new EntityAIOpenDoor(this, false));
	        this.tasks.addTask(4, new EntityAIStealItemInv(this, 1.0D, 10));
	        this.tasks.addTask(2, new EntityAIMoveTowardsRestriction(this, 1.0D));
	        this.tasks.addTask(7, new EntityAIStealFarmland(this, 0.6D));
	        this.tasks.addTask(8, new EntityAIWander(this, 1.0D));
	        this.tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
	        this.tasks.addTask(9, new EntityAILookIdle(this));

	        this.applyEntityAI();
	}

	protected void applyEntityAI()
	{
        	this.tasks.addTask(6, new EntityAIMoveThroughVillage(this, 1.0D, false));
	        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityPlayer.class, false));
	        this.targetTasks.addTask(4, new EntityAINearestAttackableTarget(this, EntityVillager.class, false));
	        this.targetTasks.addTask(4, new EntityAINearestAttackableTarget(this, EntityLiving.class, true));
	}

	protected void applyEntityAttributes()
	{
	        super.applyEntityAttributes();
	        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
	        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2D);
	        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.5D);
	        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(3.0D);
	}

	protected void entityInit()
	{
		super.entityInit();
		this.getDataManager().register(SKIN_VARIANT, "Steve");
		this.getDataManager().register(IS_CHILD, Boolean.valueOf(false));
        this.getDataManager().register(RAIDER_VARIANT,Integer.valueOf(0));
        this.getDataManager().register(RAIDER_ROLE, Integer.valueOf(0));
        this.getDataManager().register(ARMS_RAISED, Boolean.valueOf(false));
	}
	    
	public void setLeapAttack(boolean enabled)
	{
		if (this.isLeapAttackTaskSet != enabled)
		{
			this.isLeapAttackTaskSet = enabled;

			if (enabled)
			{
				this.tasks.addTask(1, this.leapAttack);
			}
			else
			{
	           this.tasks.removeTask(this.leapAttack);
			}
		}
	}
	
	
	public void setPyromaniac(boolean enabled)
	{
		if (this.isPyroTaskSet != enabled)
		{
			this.isPyroTaskSet = enabled;

			if (enabled)
			{
				this.tasks.addTask(1, this.pyromaniac);
				
			}
			else
			{
	           this.tasks.removeTask(this.pyromaniac);
			}
		}
	}
	
	public void setMelee(boolean enabled)
	{
		
		if (this.isTweaker != enabled)
		{
			this.isTweaker = enabled;

			if (enabled)
			{
				this.tasks.removeTask(this.melee);
				this.tasks.addTask(2, this.meleeTweaker);
		        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30D);
			}
			else
			{
				this.tasks.removeTask(this.meleeTweaker);
				this.tasks.addTask(2, this.melee);
		        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
			}
		}
		
	}
	
	
    @Override
    public boolean isEntityInvulnerable(DamageSource source)
    {
    	if(this.isPyroTaskSet && source == DamageSource.onFire) return false;
    	
    	return super.isEntityInvulnerable(source);
    }
	
    public void setArmsRaised(boolean armsRaised)
    {
        this.getDataManager().set(ARMS_RAISED, Boolean.valueOf(armsRaised));
    }

    @SideOnly(Side.CLIENT)
    public boolean isArmsRaised()
    {
        return ((Boolean)this.getDataManager().get(ARMS_RAISED)).booleanValue();
    }
	
    /**
     * If Animal, checks if the age timer is negative
     */
    public boolean isChild()
    {
        return ((Boolean)this.getDataManager().get(IS_CHILD)).booleanValue();
    }
    
    public void setRaiderRole(EnumRaiderRole role)
    {
    	this.getDataManager().set(RAIDER_ROLE, role.ordinal());
    }
    
    public EnumRaiderRole getRaiderRole()
    {
    	return EnumRaiderRole.get(((Integer)this.getDataManager().get(RAIDER_ROLE)).intValue());
    }
    
    @Override
    public boolean canAttackClass(Class entity) 
    {
    	if(willSteal) return false;
    	
    	if(entity == this.getClass()) return false;
    	
    	return true;
    }
    
    public void setChild(boolean childZombie)
    {
        this.getDataManager().set(IS_CHILD, Boolean.valueOf(childZombie));

        if (this.worldObj != null && !this.worldObj.isRemote)
        {
        	
            IAttributeInstance iattributeinstance = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            iattributeinstance.removeModifier(BABY_SPEED_BOOST);
            iattributeinstance.removeModifier(SPEED_BOOST_ID);

            if (childZombie)
            {
                iattributeinstance.applyModifier(BABY_SPEED_BOOST);
            }
            
        }

        this.setChildSize(childZombie);
    }
    
    public void setBrute(boolean isBrute)
    {

    	this.setBruteSize(true);
    }
    
    @Override
    public void onDeath(DamageSource cause)
    {
    	super.onDeath(cause);
    	
    	for(int i = 0; i < this.raidersInventory.getSizeInventory(); i++)
    	{
    		ItemStack stack = this.raidersInventory.getStackInSlot(i);
    		
    		if(stack != null)
    		{
    			EntityItem entity = new EntityItem(worldObj, this.posX, this.posY, this.posZ, stack);
    			
    			this.worldObj.spawnEntityInWorld(entity);
    		}
    	}
    }
    
    public void notifyDataManagerChange(DataParameter<?> key)
    {
        if (IS_CHILD.equals(key))
        {
            this.setChildSize(this.isChild());
        }
        else if(RAIDER_ROLE.equals(key))
        {
        	if(EnumRaiderRole.BRUTE == this.getRaiderRole())
        	{
        		setBruteSize(true);
        	}
        }
        
        super.notifyDataManagerChange(key);
    }
    
    protected int getExperiencePoints(EntityPlayer player)
    {
        if (this.isChild())
        {
            this.experienceValue = (int)((float)this.experienceValue * 2.5F);
        }

        return super.getExperiencePoints(player);
    }
    
    public void setChildSize(boolean isChild)
    {
        this.multiplySize(isChild ? 0.5F : 1.0F);
    }
    
    public void setBruteSize(boolean isBrute)
    {
    	this.multiplySize(isBrute ? 2F : 1.0F);
    }
    
    protected final void multiplySize(float size)
    {
        super.setSize(this.raiderWidth * size, this.raiderHeight * size);
    }
    
    protected final void multiplySize(float sizeWidth, float sizeHeight)
    {
        super.setSize(this.raiderWidth * sizeWidth, this.raiderHeight * sizeHeight);
    }
    
    protected final void setSize(float width, float height)
    {
        boolean flag = this.raiderWidth > 0.0F && this.raiderHeight > 0.0F;
        this.raiderWidth = width;
        this.raiderHeight = height;

        if (!flag)
        {
            this.multiplySize(1.0F);
        }
    }
	
    @Override
    public float getEyeHeight()
    {
        float f = 1.74F;

        if (this.isChild())
        {
            f = (float)((double)f - 0.81D);
        }

        return f;
    }
    
    public double getYOffset()
    {
        return this.isChild() ? 0.0D : -0.35D;
    }
    
    @Override
    public boolean getCanSpawnHere()
    {
    	if (this.worldObj.getDifficulty() == EnumDifficulty.PEACEFUL) return false;
    	
    	int maxEntites = (int) ((this.difficultyManager.getDay() < 4 ) ? ((this.difficultyManager.getDay() * .15) + .15) * EnumCreatureType.MONSTER.getMaxNumberOfCreature() : EnumCreatureType.MONSTER.getMaxNumberOfCreature());
    	
    	if(this.worldObj.isDaytime())
    	{

    		List<EntityRaiderBase> list = this.worldObj.getEntities(EntityRaiderBase.class,  new Predicate<EntityRaiderBase>() 
    		{
    			@Override public boolean apply(EntityRaiderBase number) 
    			{
    				return true;
    			}       
    		});
    	
    		if(list.size()+1 >= (PMSettings.daySpawnPercentage * maxEntites))
    		{
    			return false;
    		}
    	}
    	
    	
    	return true;
    }
    
    private int ScreamTick = 1200;
	@Override
	public void onLivingUpdate()
	{
		if (this.worldObj.isDaytime() && !this.worldObj.isRemote && !this.isChild())
		{
			float f = this.getBrightness(1.0F);
			BlockPos blockpos = this.getRidingEntity() instanceof EntityBoat ? (new BlockPos(this.posX, (double)Math.round(this.posY), this.posZ)).up() : new BlockPos(this.posX, (double)Math.round(this.posY), this.posZ);

		}
		IAttributeInstance iattributeinstance = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
		
		if(PMSettings.sprintersOnlyNight && !this.isChild())
		{
			if(!this.worldObj.isDaytime()  && this.getRaiderRole() != EnumRaiderRole.TWEAKER)
			{
				if(!iattributeinstance.hasModifier(NIGHT_TIME_BOOST))
				{
					iattributeinstance.applyModifier(NIGHT_TIME_BOOST);
				}

			}
			else
			{
				if(iattributeinstance.hasModifier(NIGHT_TIME_BOOST))
				{
					iattributeinstance.removeModifier(NIGHT_TIME_BOOST);
				}
			
			}

		}
		
		if(!this.worldObj.isRemote && this.getRaiderRole() == EnumRaiderRole.TWEAKER)
		{
			if(this.getAttackTarget() != null)
			{
				if(ScreamTick++ > 200)
				{
					this.worldObj.playSound(null, this.getPosition(), com.gendeathrow.pmobs.common.SoundEvents.RAIDERS_SCREAM, SoundCategory.HOSTILE, 2.0F, this.getRNG().nextFloat() * 0.4F + 0.8F);
					this.ScreamTick = this.getRNG().nextInt(100);
				}
				if(!this.isSprinting()) this.setSprinting(true);
				if(!this.isArmsRaised()) this.setArmsRaised(true);				
				
			}else if(this.getAttackTarget() == null && this.isArmsRaised())
			{
				this.setArmsRaised(false);
				if(this.isSprinting()) this.setSprinting(false);
			}
		}
		super.onLivingUpdate();
	}
	
    public boolean isNotColliding()
    {
        return this.worldObj.checkNoEntityCollision(this.getEntityBoundingBox(), this) && this.worldObj.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty();
    }

   		
    public InventoryBasic getRaidersInventory()
    {
        return this.raidersInventory;
    }
    
    @Override
    protected void updateEquipmentIfNeeded(EntityItem itemEntity)
    {
    	super.updateEquipmentIfNeeded(itemEntity);
    	
        if (!itemEntity.isDead && !this.worldObj.isRemote)
        {
  			ItemStack returnStack = this.getRaidersInventory().addItem(itemEntity.getEntityItem());
  			
  			if(returnStack == null) itemEntity.setDead();
  			else itemEntity.setEntityItemStack(returnStack);
  			
        }
    }

	/**
	 * Called when the entity is attacked.
	 */
	public boolean attackEntityFrom(DamageSource source, float amount)
	{
		if (super.attackEntityFrom(source, amount))
		{
			EntityLivingBase entitylivingbase = this.getAttackTarget();

			if (entitylivingbase == null && source.getEntity() instanceof EntityLivingBase)
			{
				entitylivingbase = (EntityLivingBase)source.getEntity();
			}

			int i = MathHelper.floor_double(this.posX);
			int j = MathHelper.floor_double(this.posY);
			int k = MathHelper.floor_double(this.posZ);

			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Called to update the entity's position/logic.
	 */
	public void onUpdate()
	{
		lastBurnTick++;
		super.onUpdate();
	}
	
	private int lastBurnTick = 0;
	public boolean attackEntityAsMob(Entity entityIn)
	{
		boolean flag = super.attackEntityAsMob(entityIn);

		if (flag)
		{
			int i = this.worldObj.getDifficulty().getDifficultyId();

			if (this.getHeldItemMainhand() == null && this.isBurning() && this.rand.nextFloat() < (float)i * 0.3F)
			{
				entityIn.setFire(2 * i);
			}
			
			if(lastBurnTick > 600 && this.getHeldItemOffhand() != null && this.getHeldItemOffhand().getItem() == Items.FLINT_AND_STEEL && this.rand.nextFloat() < (float)i * 0.3F )
			{
				this.swingArm(EnumHand.OFF_HAND);
    			this.worldObj.playSound(null, entityIn.getPosition(), SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, this.getRNG().nextFloat() * 0.4F + 0.8F);
    			this.worldObj.playSound(null, this.getPosition(), com.gendeathrow.pmobs.common.SoundEvents.RAIDERS_LAUGH, SoundCategory.HOSTILE, 1.0F, this.getRNG().nextFloat() * 0.4F + 0.8F);
				entityIn.setFire(2 * i);
				lastBurnTick = 0;
			}
		}

		return flag;
	}

	@Override
	public void dropLoot(boolean wasRecentlyHit, int lootingModifier, DamageSource source)
	{

		if(source.getEntity() != null && source.getEntity() instanceof EntityPlayer)
		{
			
			double dropit = this.rand.nextDouble();
			
			if( dropit < (.025)) //lootingModifier*0.025 + 
			{
				ItemStack stack = new ItemStack(Items.SKULL, 1, 3);
				
				if(stack.getTagCompound() == null) stack.setTagCompound(new NBTTagCompound());

				stack.getTagCompound().setString("SkullOwner", this.getOwner());

				EntityItem skull = new EntityItem(worldObj, this.posX, this.posY, this.posZ, stack);
				
				this.worldObj.spawnEntityInWorld(skull);
			}
			
			
		}

		super.dropLoot(wasRecentlyHit, lootingModifier, source);
	}
	

	protected SoundEvent getAmbientSound()
	{
		if(this.getRNG().nextDouble() < .25 && this.isPyroTaskSet) return com.gendeathrow.pmobs.common.SoundEvents.RAIDERS_LAUGH;
		return com.gendeathrow.pmobs.common.SoundEvents.RAIDERS_SAY;
	}

	protected SoundEvent getHurtSound()
	{
		return com.gendeathrow.pmobs.common.SoundEvents.RAIDERS_HURT;
	}

	protected SoundEvent getDeathSound()
	{
		return SoundEvents.ENTITY_PLAYER_DEATH;
	}

	protected void playStepSound(BlockPos pos, Block blockIn)
	{
		this.playSound(SoundEvents.ENTITY_ZOMBIE_STEP, 0.15F, 1.0F);
	}

	/**
	 * Get this Entity's EnumCreatureAttribute
	 */
	public EnumCreatureAttribute getCreatureAttribute()
	{
		return EnumCreatureAttribute.UNDEFINED;
	}

	@Nullable
	protected ResourceLocation getLootTable()
	{
		return RaidersCore.playerraidersloot;
	}
	    
	/**
	 * Called only once on an entity when first time spawned, via egg, mob spawner, natural spawning etc, but not called
	 * when entity is reloaded from nbt. Mainly used for initializing attributes and inventory
	 */
	@Nullable
	public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata)
	{
	        livingdata = super.onInitialSpawn(difficulty, livingdata);
	        
	        //this.tasks.addTask(2, melee);
	        
	        float f = difficulty.getClampedAdditionalDifficulty();
	        
	        this.setRandomFeatures(); 
	        
	        this.setCanPickUpLoot(true);
	      
	        this.playerProfile = RaiderManager.getRandomRaider().getProfile();

	        setOwner(this.playerProfile != null ? this.playerProfile.getName() : "Steve");

	        this.setCustomNameTag(getPlayerProfile().getName());
	        
	        // Make sure they can walk though doors
	        ((PathNavigateGround)this.getNavigator()).setEnterDoors(true);

	        ((PathNavigateGround)this.getNavigator()).setBreakDoors(true);

	        if(this.worldObj.rand.nextFloat() < net.minecraftforge.common.ForgeModContainer.zombieBabyChance && !this.getOwner().equalsIgnoreCase("herobrine") && !((EntityRangedAttacker)this).isRangedAttacker) this.setChild(true); 
        	
	        this.setAlwaysRenderNameTag(false);
	        
	        this.setEquipmentBasedOnDifficulty(difficulty);
	        
	        this.setEnchantmentBasedOnDifficulty(difficulty);

	        if (this.getItemStackFromSlot(EntityEquipmentSlot.HEAD) == null)
	        {
	            Calendar calendar = this.worldObj.getCurrentDate();

	            if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31 && this.rand.nextFloat() < 0.25F)
	            {
	                this.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(this.rand.nextFloat() < 0.1F ? Blocks.LIT_PUMPKIN : Blocks.PUMPKIN));
	                this.inventoryArmorDropChances[EntityEquipmentSlot.HEAD.getIndex()] = 0.0F;
	            }
	        }
	        
	        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).applyModifier(new AttributeModifier("Random spawn bonus", this.rand.nextDouble() * 0.05000000074505806D, 0));
	        
	        double d0 = this.rand.nextDouble() * 1.5D * (double)f;

	        if (d0 > 1.0D)
	        { 
	            this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).applyModifier(new AttributeModifier("Random zombie-spawn bonus", d0, 2));
	        }


        
	        if(PMSettings.leapAttackAI && rand.nextDouble() < .15 + difficultyManager.calculateProgressionDifficulty(.05, .35))
	        {
	        	this.setLeapAttack(true);
	        }
	        
	        if(rand.nextDouble() < 0.05D + difficultyManager.calculateProgressionDifficulty(.05, .15) && !this.isPyroTaskSet)
	        {
	        	this.setMelee(true);
	        	this.setRaiderRole(EnumRaiderRole.TWEAKER);
	        	this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).applyModifier(new AttributeModifier("Tweaker Health", -.5, 2));
	        	
	        }
	        else this.setMelee(false);
	        
	        if(PMSettings.pyroAI && rand.nextDouble() < 0.05D + difficultyManager.calculateProgressionDifficulty(.025, .10) && !this.isTweaker)
	        {
	        	this.setPyromaniac(true);
		        this.setRaiderRole(EnumRaiderRole.PYROMANIAC);
	        	this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.FLINT_AND_STEEL));
	        }
	        
	        if(rand.nextDouble() < 0.75D + difficultyManager.calculateProgressionDifficulty(.025, .10) && !this.isTweaker)
	        {
		        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(.5);
		        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(.15);
		        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30);
		        this.setBrute(true);
		        this.setRaiderRole(EnumRaiderRole.BRUTE);
	        }
	        	


	        difficultyManager.setHealthDifficulty(difficulty);
	        
	        if(!this.isChild())
	        {
	        	difficultyManager.setSpeedDifficulty(difficulty);
	        }
	        
	        
	        this.setHealth(this.getMaxHealth());

	        return livingdata;
	}
	
		private DifficultyProgression getDifficultyProgession()
		{
			return this.difficultyManager;
		}
	    
	    @Override
	    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty)
	    {

	    	if (this.rand.nextFloat() < (0.25F * difficulty.getClampedAdditionalDifficulty()) + this.difficultyManager.calculateProgressionDifficulty(.035))
	        {
	            int i = this.rand.nextInt(2);
	            float f = this.worldObj.getDifficulty() == EnumDifficulty.HARD ? PMSettings.setEquptmentHard : PMSettings.setEquitmentDefault;

	            boolean armorflag = true;
	            boolean handflag = true;

	            ArmorSetWeigthedItem ArmorSet = EquipmentManager.getRandomArmor();
	            
	            for (EntityEquipmentSlot entityequipmentslot : EntityEquipmentSlot.values())
	            {
	                if (entityequipmentslot.getSlotType() == EntityEquipmentSlot.Type.ARMOR)
	                {
	                    ItemStack itemstack = this.getItemStackFromSlot(entityequipmentslot);

	                    if (!armorflag && this.rand.nextFloat() > f + this.difficultyManager.calculateProgressionDifficulty(.035))
	                    {
	                        break;
	                    }

	                    armorflag = false;

	                    if (itemstack == null & ArmorSet != null)
	                    {
	                   		ItemStack stack = ArmorSet.getArmorbyEquipmentSlot(entityequipmentslot);
	                   		
	                        if (stack != null && stack.getItem() != null)
	                        {
	                        	this.setItemStackToSlot(entityequipmentslot, stack.copy());
	                        }
	                    }
	                }
	                else if(entityequipmentslot.getSlotType() == EntityEquipmentSlot.Type.HAND)
	                {
	                    ItemStack itemstack = this.getItemStackFromSlot(entityequipmentslot);

	                    if (!handflag && this.rand.nextFloat() > f + this.difficultyManager.calculateProgressionDifficulty(.035))
	                    {
	                        break;
	                    }

	                    handflag = false;

	                    if (itemstack == null)
	                    {
	                    	ItemStack stack = null;
	                    	
	                    	if(entityequipmentslot == EntityEquipmentSlot.MAINHAND)
	                    	{
	                    		stack = EquipmentManager.getRandomWeapons().getCopy();
	                    	}
	                    	else if(entityequipmentslot == EntityEquipmentSlot.OFFHAND)
	                    	{
	                    		stack = EquipmentManager.getRandomOffHand().getCopy();
	                    	}
	                        if (stack != null && stack.getItem() != null)
	                        {
	                            this.setItemStackToSlot(entityequipmentslot, stack);
	                        }
	                    }
	                }
	                
	            }
	        } 
	    }
	    
	    
	    /**
	     * (abstract) Protected helper method to read subclass entity data from NBT.
	     */
	    @Override
	    public void readEntityFromNBT(NBTTagCompound compound)
	    {
	        super.readEntityFromNBT(compound);

	        if (compound.hasKey("Owner"))
	        {
	            this.playerProfile = NBTUtil.readGameProfileFromNBT(compound.getCompoundTag("Owner"));
	            //System.out.println(compound.getCompoundTag("Owner"));
	            this.setOwner(playerProfile.getName());
	            
	            this.updatePlayerProfile();
	        }
	        
	        if (compound.getBoolean("IsBaby"))
	        {
	            this.setChild(true);
	        }
	        
	        if(compound.hasKey("OverlayType"))
	        {
	        	this.setFeatures(compound.getInteger("OverlayType"));
	        }
	        
	        NBTTagList nbttaglist = compound.getTagList("Inventory", this.raidersInventory.getSizeInventory());
	        
	        for (int i = 0; i < nbttaglist.tagCount(); ++i)
	        {
	            ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbttaglist.getCompoundTagAt(i));

	            if (itemstack != null)
	            {
	                this.raidersInventory.addItem(itemstack);
	            }
	        }


	    }

	    /**
	     * (abstract) Protected helper method to write subclass entity data to NBT.
	     */
	    @Override
	    public void writeEntityToNBT(NBTTagCompound compound)
	    {
	        super.writeEntityToNBT(compound);
	        if (this.playerProfile != null)
	        {
	            NBTTagCompound nbttagcompound = new NBTTagCompound();
	            NBTUtil.writeGameProfile(nbttagcompound, this.playerProfile);
	            compound.setTag("Owner", nbttagcompound);
	        }
	        
	        if (this.isChild())
	        {
	            compound.setBoolean("IsBaby", true);
	        }
	        
	        compound.setInteger("OverlayType", this.getFeatures().ordinal());
	        
	        NBTTagList nbttaglist = new NBTTagList();

	        for (int i = 0; i < this.raidersInventory.getSizeInventory(); ++i)
	        {
	            ItemStack itemstack = this.raidersInventory.getStackInSlot(i);

	            if (itemstack != null)
	            {
	                nbttaglist.appendTag(itemstack.writeToNBT(new NBTTagCompound()));
	            }
	        }

	        compound.setTag("Inventory", nbttaglist);
	    }

		private GameProfile randomSkin()
		{
			Object[] profiles = RaiderManager.raidersList.values().toArray();
			return (GameProfile) profiles[rand.nextInt(profiles.length)];
		}
		
		@Deprecated		
		public static void setProfileCache(PlayerProfileCache cache)
		{
			profileCache = cache;
		}
		
		@Deprecated
		public static void setSessionService(MinecraftSessionService minecraftSessionService)
		{
			sessionService = minecraftSessionService;
		}
	    
	    public void setEntitySkin(ResourceLocation skinIn)
	    {
	    	this.SKIN = skinIn;
	    }
	    
	    // player model or new skin. 
	    public boolean isPlayerSkin()
	    {
	    	return false;
	    }
	    
	    /**
	     * 	Layer Features are not synced
	     */
	    //@SideOnly(Side.CLIENT)
	    public void setRandomFeatures()
	    {
	    	setFeatures(LayerFeatures.randomFeature(this.rand).ordinal()); 
	    }
	    
	    public void setFeatures(int ordinal)
	    {
	    	this.dataManager.set(this.RAIDER_VARIANT, ordinal);
	    }
	    
	    @SideOnly(Side.CLIENT)
	    public LayerFeatures getFeatures()
	    {
			return LayerFeatures.values()[this.dataManager.get(RAIDER_VARIANT).intValue()]; 
	    }
	    
	    /**
	     * Returns true if the player instance has an associated skin.
	     */
	    public ResourceLocation getLocationSkin()
	    {
	    	ResourceLocation resourcelocation = DefaultPlayerSkin.getDefaultSkinLegacy();

			if(this.playerProfile == null || !this.playerProfile.isComplete())
	    	{
	    		this.setPlayerProfile(this.getOwner());
	    	}

			if(RaidersSkinManager.INSTANCE.cachedSkins.containsKey(this.playerProfile.getName()))
			{
				resourcelocation = RaidersSkinManager.INSTANCE.cachedSkins.get(this.playerProfile.getName());
			}

			return resourcelocation;
	    }  
	    
	    public GameProfile updateGameprofile(GameProfile input)
	    {
	    	
	    	return RaidersSkinManager.profileCache.getGameProfileForUsername((input.getName()));
	    }
	    
	    
	    public void setOwner(String name)
	    {
	        this.dataManager.set(SKIN_VARIANT, name);
	    }
	    
	    private void setPlayerProfile(String name) 
	    {
	    	this.setPlayerProfile(RaiderManager.getRaiderProfile(name));
		}

		public String getOwner()
	    {
	    	return this.dataManager.get(SKIN_VARIANT);
	    }
	    
	    public GameProfile getPlayerProfile()
	    {
    		return this.playerProfile;
	    }
	    
	    @Deprecated
	    public void setPlayerProfile(@Nullable GameProfile playerProfile)
	    {
	        this.playerProfile = playerProfile;
	    }

	    private void updatePlayerProfile()
	    {
	    		RaidersSkinManager.updateProfile(this);
	    }

	    protected boolean profileset = false;
	    
		public void setProfileUpdated(boolean b) 
		{
			profileset = b;
		}
	    
		public enum EnumRaiderRole
		{
			PYROMANIAC(0.10, 0.05, 0.15, 0),
			TWEAKER(0.10,0.05,0.15, 0),
			BRUTE(.10,.01, 0);

			private double chance;
			private double chanceIncrease;
			private double maxChanceToSpawn;
			private int startDifficulty;
			
			
			EnumRaiderRole(double chance, double chanceIncrease, double max)
			{
				
				this(chance, chanceIncrease, max, 0);
			}
			
			public static EnumRaiderRole get(int intValue) 
			{
				for(EnumRaiderRole role : EnumRaiderRole.values())
				{
					if (role.ordinal() == intValue) return role;
				}
				return null;
			}

			EnumRaiderRole(double chance, double chanceIncrease, double max, int startDifficulty)
			{
				this.chance = chance;
				this.chanceIncrease = chanceIncrease;
				this.maxChanceToSpawn = max;
				this.startDifficulty = startDifficulty;
			}
			
			
			public static EnumRaiderRole getRandomRole(EntityRaiderBase raider, DifficultyProgression manager)
			{
				for(EnumRaiderRole Role : EnumRaiderRole.values())
				{
					if(raider.getRNG().nextDouble() < Role.chance + manager.calculateProgressionDifficulty(Role.chanceIncrease, Role.startDifficulty, Role.maxChanceToSpawn))
					{
						return Role;
					}
				}
				
				return null;
			}
		}
}
