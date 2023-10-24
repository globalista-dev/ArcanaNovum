package net.borisshoes.arcananovum.mixins;

import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.Arcananovum;
import net.borisshoes.arcananovum.achievements.ArcanaAchievements;
import net.borisshoes.arcananovum.augments.ArcanaAugments;
import net.borisshoes.arcananovum.bosses.BossFights;
import net.borisshoes.arcananovum.callbacks.ShieldTimerCallback;
import net.borisshoes.arcananovum.callbacks.TickTimerCallback;
import net.borisshoes.arcananovum.core.MagicItem;
import net.borisshoes.arcananovum.effects.DamageAmpEffect;
import net.borisshoes.arcananovum.items.*;
import net.borisshoes.arcananovum.items.charms.CindersCharm;
import net.borisshoes.arcananovum.items.charms.FelidaeCharm;
import net.borisshoes.arcananovum.utils.GenericTimer;
import net.borisshoes.arcananovum.utils.MagicItemUtils;
import net.borisshoes.arcananovum.utils.ParticleEffectUtils;
import net.borisshoes.arcananovum.utils.SoundUtils;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import static net.borisshoes.arcananovum.Arcananovum.SERVER_TIMER_CALLBACKS;
import static net.borisshoes.arcananovum.cardinalcomponents.PlayerComponentInitializer.PLAYER_DATA;
import static net.borisshoes.arcananovum.cardinalcomponents.WorldDataComponentInitializer.BOSS_FIGHT;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
   
   @Shadow protected abstract void playBlockFallSound();
   
   @Shadow protected abstract void playHurtSound(DamageSource source);
   
   // Mixin for Shield of Fortitude giving absorption hearts
   @Inject(method="damage",at=@At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damageShield(F)V"))
   private void arcananovum_shieldAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
      LivingEntity entity = (LivingEntity) (Object) this;
      ItemStack activeItem = entity.getActiveItem();
      MagicItem magic;
      ItemStack item = null;
      
      if(MagicItemUtils.isMagic(activeItem)){
         magic = MagicItemUtils.identifyItem(activeItem);
         item = activeItem;
      }else{
         return;
      }
      if(magic instanceof ShieldOfFortitude shield){
         float maxAbs = 10 + 2*Math.max(0,ArcanaAugments.getAugmentOnItem(item,ArcanaAugments.SHIELD_OF_FAITH.id));
         float curAbs = entity.getAbsorptionAmount();
         float addedAbs = (float) Math.min(maxAbs,amount*.5);
         int duration = 200 + 100*Math.max(0,ArcanaAugments.getAugmentOnItem(item,ArcanaAugments.SHIELD_OF_RESILIENCE.id));
         if(entity instanceof ServerPlayerEntity player){
            Arcananovum.addTickTimerCallback(new ShieldTimerCallback(duration,item,player,addedAbs));
            SoundUtils.playSongToPlayer(player,SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1.8f);
         }
         entity.setAbsorptionAmount((curAbs + addedAbs));
      }
   }
   
   @Inject(method="damage",at=@At(value = "INVOKE", target = "Lnet/minecraft/world/World;getTime()J"))
   private void arcananovum_playerDamaged(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
      LivingEntity entity = (LivingEntity) (Object) this;
      if(entity instanceof ServerPlayerEntity player){
         PlayerInventory inv = player.getInventory();
         for(int i=0; i<inv.size();i++){
            ItemStack item = inv.getStack(i);
            if(item.isEmpty()){
               continue;
            }
      
            boolean isMagic = MagicItemUtils.isMagic(item);
            if(!isMagic)
               continue; // Item not magic, skip
      
            
         }
   
         // Stall Levitation Harness
         ItemStack chestItem = entity.getEquippedStack(EquipmentSlot.CHEST);
         if(MagicItemUtils.isMagic(chestItem) && player.getAbilities().flying){
            if(MagicItemUtils.identifyItem(chestItem) instanceof LevitationHarness harness){
               int sturdyLvl = Math.max(0,ArcanaAugments.getAugmentOnItem(chestItem,ArcanaAugments.STURDY_CONSTRUCTION.id));
               final double[] sturdyChance = {0,.15,.35,.5};
               if(Math.random() >= sturdyChance[sturdyLvl]){
                  int rebootLvl = Math.max(0,ArcanaAugments.getAugmentOnItem(chestItem,ArcanaAugments.FAST_REBOOT.id));
                  harness.setStall(chestItem,10-2*rebootLvl);
                  player.setHealth(player.getHealth()/2);
                  player.sendMessage(Text.literal("Your Harness Stalls!").formatted(Formatting.YELLOW,Formatting.ITALIC),true);
                  SoundUtils.playSound(player.getServerWorld(),player.getBlockPos(),SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS,1, 0.7f);
                  ParticleEffectUtils.harnessStall(player.getServerWorld(),player.getPos().add(0,0.5,0));
                  
                  boolean eProt = Math.max(0,ArcanaAugments.getAugmentOnItem(chestItem,ArcanaAugments.EMERGENCY_PROTOCOL.id)) >= 1;
                  if(eProt){
                     StatusEffectInstance levit = new StatusEffectInstance(StatusEffects.LEVITATION, 100, 0, false, false, true);
                     player.addStatusEffect(levit);
                  }
               }
            }
         }
      }
   }
   
   // Mixin for shadow stalker's glaive doing damage
   @Inject(method="damage",at=@At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"))
   private void arcananovum_damageDealt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
      LivingEntity entity = (LivingEntity) (Object) this;
      Entity attacker = source.getAttacker();
      if(attacker instanceof ServerPlayerEntity player){
         ItemStack weapon = player.getEquippedStack(EquipmentSlot.MAINHAND);
   
         if(MagicItemUtils.identifyItem(weapon) instanceof ShadowStalkersGlaive glaive){
            int oldEnergy = glaive.getEnergy(weapon);
            glaive.addEnergy(weapon, (int) amount);
            int newEnergy = glaive.getEnergy(weapon);
            if(oldEnergy/20 != newEnergy/20){
               String message = "Glaive Charges: ";
               for(int i=1; i<=5; i++){
                  message += newEnergy >= i*20 ? "✦ " : "✧ ";
               }
               player.sendMessage(Text.literal(message).formatted(Formatting.BLACK),true);
            }
         }
      }
   }
   
   
   // Mixin for damage modifiers
   @Inject(method = "modifyAppliedDamage", at = @At("RETURN"), cancellable = true)
   private void arcananovum_modifyDamage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir){
      float newReturn = cir.getReturnValueF();
      LivingEntity entity = (LivingEntity) (Object) this;
      Entity attacker = source.getAttacker();
      
      if(attacker instanceof ServerPlayerEntity player){
         // Juggernaut Augment
         ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
         if(MagicItemUtils.identifyItem(boots) instanceof SojournerBoots sojournerBoots){
            boolean juggernaut = ArcanaAugments.getAugmentOnItem(boots,ArcanaAugments.JUGGERNAUT.id) >= 1;
            int energy = sojournerBoots.getEnergy(boots);
            if(juggernaut && energy >= 200){
               StatusEffectInstance slow = new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 4, false, false, true);
               StatusEffectInstance dmgAmp = new StatusEffectInstance(ArcanaRegistry.DAMAGE_AMP_EFFECT, 100, 1, false, false, false);
               entity.addStatusEffect(slow);
               entity.addStatusEffect(dmgAmp);
               sojournerBoots.setEnergy(boots,0);
               SoundUtils.playSound(player.getServerWorld(),entity.getBlockPos(),SoundEvents.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, .5f, .8f);
            }
         }
         
         
         // Shield Bash Augment
         ItemStack shieldStack = null;
         if(MagicItemUtils.identifyItem(player.getEquippedStack(EquipmentSlot.OFFHAND)) instanceof ShieldOfFortitude){
            shieldStack = player.getEquippedStack(EquipmentSlot.OFFHAND);
         }else if(MagicItemUtils.identifyItem(player.getEquippedStack(EquipmentSlot.MAINHAND)) instanceof ShieldOfFortitude){
            shieldStack = player.getEquippedStack(EquipmentSlot.MAINHAND);
         }
         if(shieldStack != null && ArcanaAugments.getAugmentOnItem(shieldStack,ArcanaAugments.SHIELD_BASH.id) >= 1){
            ArrayList<TickTimerCallback> toRemove = new ArrayList<>();
            float shieldTotal = 0;
            float absAmt = player.getAbsorptionAmount();
            for(int i = 0; i < SERVER_TIMER_CALLBACKS.size(); i++){
               TickTimerCallback t = SERVER_TIMER_CALLBACKS.get(i);
               if(t instanceof ShieldTimerCallback st){
                  if(st.getPlayer().getUuidAsString().equals(player.getUuidAsString())){
                     shieldTotal += st.getHearts();
                     st.onTimer();
                     toRemove.add(st);
                  }
               }
            }
            shieldTotal = Math.min(Math.min(absAmt,shieldTotal),50);
            if(shieldTotal >= 10){
               StatusEffectInstance slow = new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 4, false, false, true);
               StatusEffectInstance dmgAmp = new StatusEffectInstance(ArcanaRegistry.DAMAGE_AMP_EFFECT, 100, 1, false, false, false);
               entity.addStatusEffect(slow);
               entity.addStatusEffect(dmgAmp);
               SERVER_TIMER_CALLBACKS.removeIf(toRemove::contains); // Remove all absorption callbacks
               int duration = 200 + 100*Math.max(0,ArcanaAugments.getAugmentOnItem(shieldStack,ArcanaAugments.SHIELD_OF_RESILIENCE.id));
               Arcananovum.addTickTimerCallback(new ShieldTimerCallback(duration,shieldStack,player,10)); // Put 5 hearts back
               player.setAbsorptionAmount(player.getAbsorptionAmount() + 10f);
               player.getItemCooldownManager().set(ArcanaRegistry.SHIELD_OF_FORTITUDE.getItem(),60);
               SoundUtils.playSound(player.getServerWorld(),entity.getBlockPos(),SoundEvents.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, .5f, .8f);
            }
         }
      }
      
      // Damage Amp
      if(entity.hasStatusEffect(ArcanaRegistry.DAMAGE_AMP_EFFECT)){
         StatusEffectInstance effect = entity.getStatusEffect(ArcanaRegistry.DAMAGE_AMP_EFFECT);
         DamageAmpEffect.tryTrackDamage(effect.getAmplifier(),newReturn,entity);
         newReturn = DamageAmpEffect.buffDamage(newReturn,effect.getAmplifier());
      }
   
      if(entity instanceof ServerPlayerEntity player){
         List<Pair<List<ItemStack>,ItemStack>> allItems = new ArrayList<>();
         PlayerInventory playerInv = player.getInventory();
         boolean procdFelidae = false;
         
         List<ItemStack> invItems = new ArrayList<>();
         for(int i=0; i<playerInv.size();i++){
            ItemStack item = playerInv.getStack(i);
            if(item.isEmpty()){
               continue;
            }
         
            invItems.add(item);
            MagicItem magicItem = MagicItemUtils.identifyItem(item);
            if(magicItem instanceof ArcanistsBelt belt){
               SimpleInventory beltInv = belt.deserialize(item);
               ArrayList<ItemStack> beltList = new ArrayList<>();
               for(int j = 0; j < beltInv.size(); j++){
                  beltList.add(beltInv.getStack(j));
               }
               allItems.add(new Pair<>(beltList,item));
            }
         }
         allItems.add(new Pair<>(invItems,ItemStack.EMPTY));
         
         for(int i = 0; i < allItems.size(); i++){
            List<ItemStack> itemList = allItems.get(i).getLeft();
            ItemStack carrier = allItems.get(i).getRight();
            SimpleInventory sinv = new SimpleInventory(itemList.size());
            
            for(int j = 0; j < itemList.size(); j++){
               ItemStack item = itemList.get(j);
               
               boolean isMagic = MagicItemUtils.isMagic(item);
               if(!isMagic)
                  continue; // Item not magic, skip
               MagicItem magicItem = MagicItemUtils.identifyItem(item);
               
               if((magicItem instanceof FelidaeCharm) && source.isIn(DamageTypeTags.IS_FALL) && !procdFelidae){ // Felidae Charm
                  int graceLvl = Math.max(0,ArcanaAugments.getAugmentOnItem(item,ArcanaAugments.FELINE_GRACE.id));
                  float dmgMod = (float) (0.5 - 0.125*graceLvl);
                  SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_CAT_PURREOW, 1,1);
                  float oldReturn = newReturn;
                  newReturn = newReturn * dmgMod < 2 ? 0 : newReturn * dmgMod; // Reduce the damage, if the remaining damage is less than a heart, remove all of it.
                  PLAYER_DATA.get(player).addXP(10*(int)(oldReturn-newReturn)); // Add xp
                  if(oldReturn > player.getHealth() && newReturn < player.getHealth()) ArcanaAchievements.grant(player,ArcanaAchievements.LAND_ON_FEET.id);
                  procdFelidae = true; // Make it so multiple charms don't stack
                  
               }else if(magicItem instanceof PearlOfRecall){ // Cancel all Pearls of Recall
                  NbtCompound itemNbt = item.getNbt();
                  NbtCompound magicNbt = itemNbt.getCompound("arcananovum");
                  int defenseLvl = Math.max(0, ArcanaAugments.getAugmentOnItem(item,ArcanaAugments.PHASE_DEFENSE.id));
                  final double[] defenseChance = {0,.15,.35,.5};
                  
                  if(magicNbt.getInt("heat") > 0){
                     if(Math.random() >= defenseChance[defenseLvl]){
                        player.sendMessage(Text.literal("Your Recall Has Been Disrupted!").formatted(Formatting.RED, Formatting.ITALIC), true);
                        magicNbt.putInt("heat", -1);
                     }else{
                        newReturn = 0;
                     }
                  }
               }else if(magicItem instanceof Planeshifter){ // Cancel all Planeshifters
                  NbtCompound itemNbt = item.getNbt();
                  NbtCompound magicNbt = itemNbt.getCompound("arcananovum");
                  
                  if(magicNbt.getInt("heat") > 0){
                     player.sendMessage(Text.literal("Your Plane-Shift Has Been Disrupted!").formatted(Formatting.RED, Formatting.ITALIC), true);
                     magicNbt.putInt("heat", -1);
                  }
               }else if(magicItem instanceof CindersCharm cinders && source.isIn(DamageTypeTags.IS_FIRE)){ // Cinders Charm Cremation
                  boolean cremation = Math.max(0,ArcanaAugments.getAugmentOnItem(item,"cremation")) >= 1;
                  if(cremation){
                     float oldReturn = newReturn;
                     int energy = cinders.getEnergy(item);
                     float dmgReduction = (float) Math.min(energy / 4.0, oldReturn);
                     newReturn = oldReturn - dmgReduction;
                     cinders.addEnergy(item, (int) -dmgReduction * 4);
                     
                     energy = cinders.getEnergy(item);
                     StringBuilder message = new StringBuilder("Cinders: ");
                     for(int k = 1; k <= cinders.getMaxEnergy(item)/20; k++){
                        message.append(energy >= k * 20 ? "✦ " : "✧ ");
                     }
                     player.sendMessage(Text.literal(message.toString()).formatted(Formatting.AQUA), true);
                  }
               }
               
               sinv.setStack(j,item);
            }
            
            if(MagicItemUtils.identifyItem(carrier) instanceof ArcanistsBelt belt){
               belt.serialize(carrier, sinv);
            }
         }
      }
      
      ItemStack chestItem = entity.getEquippedStack(EquipmentSlot.CHEST);
      if(MagicItemUtils.identifyItem(chestItem) instanceof WingsOfEnderia wings){
         boolean canReduce = source.isIn(DamageTypeTags.IS_FALL) || source.getName().equals("flyIntoWall") || ArcanaAugments.getAugmentOnItem(chestItem,ArcanaAugments.SCALES_OF_THE_CHAMPION.id) >= 2;
         if(canReduce){
            int energy = wings.getEnergy(chestItem);
            float maxDmgReduction = newReturn * .5f;
            float dmgReduction = (float) Math.min(energy / 100.0, maxDmgReduction);
            if(entity instanceof ServerPlayerEntity player){
               if(dmgReduction == maxDmgReduction || dmgReduction > 12){
                  if(source.isIn(DamageTypeTags.IS_FALL) || source.getName().equals("flyIntoWall")){
                     player.sendMessage(Text.literal("Your Armored Wings cushion your fall!").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC), true);
                  }
                  SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1, 1.3f);
                  Arcananovum.addTickTimerCallback(new GenericTimer(50, new TimerTask() {
                     @Override
                     public void run(){
                        player.sendMessage(Text.literal("Wing Energy Remaining: " + wings.getEnergy(chestItem)).formatted(Formatting.DARK_PURPLE), true);
                     }
                  }));
               }
               PLAYER_DATA.get(player).addXP((int) dmgReduction * 25); // Add xp
               if(source.getName().equals("flyIntoWall") && newReturn > player.getHealth() && (newReturn - dmgReduction) < player.getHealth())
                  ArcanaAchievements.grant(player, ArcanaAchievements.SEE_GLASS.id);
            }
            wings.addEnergy(chestItem, (int) -dmgReduction * 100);
            newReturn -= dmgReduction;
         }
         
         // Wing Buffet ability
         double buffetChance = new double[]{0,.1,.2,.3,.4,1}[Math.max(0,ArcanaAugments.getAugmentOnItem(chestItem,ArcanaAugments.WING_BUFFET.id))];
         if(entity instanceof ServerPlayerEntity player && Math.random() < buffetChance){
            ServerWorld world = player.getServerWorld();
            Vec3d pos = player.getPos().add(0,player.getHeight()/2,0);
            Box rangeBox = new Box(pos.x+8,pos.y+8,pos.z+8,pos.x-8,pos.y-8,pos.z-8);
            int range = 3;
            List<Entity> entities = world.getOtherEntities(entity,rangeBox, e -> !e.isSpectator() && e.squaredDistanceTo(pos) < 1.5*range*range && (e instanceof MobEntity));
            for(Entity entity1 : entities){
               if(wings.getEnergy(chestItem) < 50) break;
               Vec3d diff = entity1.getPos().subtract(pos);
               double multiplier = MathHelper.clamp(range*.75-diff.length()*.5,.1,3);
               Vec3d motion = diff.multiply(1,0,1).add(0,1,0).normalize().multiply(multiplier);
               entity1.setVelocity(motion.x,motion.y,motion.z);
               SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1, .7f);
               wings.addEnergy(chestItem,-50);
            }
         }
      }
   
      
      // Enderia Boss health scale
      Pair<BossFights, NbtCompound> bossFight = BOSS_FIGHT.get(entity.getWorld()).getBossFight();
      int numPlayers = 0;
      if(bossFight != null){
         numPlayers = bossFight.getRight().getInt("numPlayers");
      }
      if(numPlayers != 0){
         float scale = 2f/numPlayers;
         if(entity instanceof EnderDragonEntity){
            newReturn *= scale; //Effective Health Scale to bypass 1024 hp cap
            if(source.isIn(DamageTypeTags.BYPASSES_ARMOR) || source.isIn(DamageTypeTags.IS_EXPLOSION)) newReturn *= 0.25f; // Reduce damage from magic and explosive sources
         }
      }
      
      cir.setReturnValue(newReturn);
   }
   
   @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
   public void arcananovum_useTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir){
      LivingEntity livingEntity = (LivingEntity) (Object) this;
   
      if(source.getName().contains("ArcanaNovum.Concentration")){ // Allow totem usage on Concentration damage
         ItemStack itemStack = null;
         Hand[] var4 = Hand.values();
         int var5 = var4.length;
   
         for(int var6 = 0; var6 < var5; ++var6) {
            Hand hand = var4[var6];
            ItemStack itemStack2 = livingEntity.getStackInHand(hand);
            if (itemStack2.isOf(Items.TOTEM_OF_UNDYING)) {
               itemStack = itemStack2.copy();
               itemStack2.decrement(1);
               break;
            }
         }
   
         if (itemStack != null) {
            if (livingEntity instanceof ServerPlayerEntity serverPlayerEntity) {
               serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(Items.TOTEM_OF_UNDYING));
               Criteria.USED_TOTEM.trigger(serverPlayerEntity, itemStack);
   
               PlayerInventory inv = serverPlayerEntity.getInventory();
               for(int i=0; i<inv.size();i++){
                  ItemStack item = inv.getStack(i);
                  if(item.isEmpty()){
                     continue;
                  }
   
                  boolean isMagic = MagicItemUtils.isMagic(item);
                  if(!isMagic)
                     continue; // Item not magic, skip
                  MagicItem magicItem = MagicItemUtils.identifyItem(item);
   
                  if(magicItem instanceof NulMemento nulMemento){ // Nul Memento Totem Death's Door
                     if(nulMemento.isActive(item)){
                        ArcanaAchievements.grant(serverPlayerEntity,ArcanaAchievements.DEATHS_DOOR.id);
                     }
                  }
               }
            }
      
            livingEntity.setHealth(1.0F);
            livingEntity.clearStatusEffects();
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
            livingEntity.getWorld().sendEntityStatus(livingEntity, (byte)35);
         }
   
         cir.setReturnValue(itemStack != null);
      }
   }
   
   @Redirect(method="tickFallFlying", at=@At(value="INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
   private boolean arcananovum_elytraTick(ItemStack stack, Item item){
      return stack.isOf(item) || MagicItemUtils.identifyItem(stack) instanceof WingsOfEnderia;
   }
   
   @Inject(method="updatePotionVisibility", at=@At(value="INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setInvisible(Z)V", ordinal = 1, shift = At.Shift.AFTER))
   private void arcananovum_greaterInvisibilityUpdate(CallbackInfo ci){
      LivingEntity livingEntity = (LivingEntity) (Object) this;
      livingEntity.setInvisible(livingEntity.isInvisible() || livingEntity.hasStatusEffect(ArcanaRegistry.GREATER_INVISIBILITY_EFFECT));
   }
}
