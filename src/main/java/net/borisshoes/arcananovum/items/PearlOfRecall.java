package net.borisshoes.arcananovum.items;

import net.borisshoes.arcananovum.recipes.MagicItemRecipe;
import net.borisshoes.arcananovum.utils.MagicRarity;
import net.borisshoes.arcananovum.utils.ParticleEffectUtils;
import net.borisshoes.arcananovum.utils.SoundUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static net.borisshoes.arcananovum.cardinalcomponents.PlayerComponentInitializer.PLAYER_DATA;

public class PearlOfRecall extends EnergyItem implements TickingItem, UsableItem{
   
   public PearlOfRecall(){
      id = "pearl_of_recall";
      name = "Pearl of Recall";
      rarity = MagicRarity.EXOTIC;
      maxEnergy = 600; // 10 minute recharge time
      initEnergy = 600;
   
      ItemStack item = new ItemStack(Items.ENDER_EYE);
      NbtCompound tag = item.getOrCreateNbt();
      NbtCompound display = new NbtCompound();
      NbtList loreList = new NbtList();
      NbtList enchants = new NbtList();
      enchants.add(new NbtCompound()); // Gives enchant glow with no enchants
      display.putString("Name","[{\"text\":\"Pearl of Recall\",\"italic\":false,\"bold\":true,\"color\":\"dark_aqua\"}]");
      loreList.add(NbtString.of("[{\"text\":\"An \",\"italic\":false,\"color\":\"green\"},{\"text\":\"Ender Pearl\",\"color\":\"dark_aqua\"},{\"text\":\" whose \"},{\"text\":\"moment \",\"color\":\"blue\"},{\"text\":\"of \"},{\"text\":\"activation \",\"color\":\"dark_green\"},{\"text\":\"was \"},{\"text\":\"frozen \",\"color\":\"aqua\"},{\"text\":\"for later use.\",\"color\":\"green\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"It requires the \",\"italic\":false,\"color\":\"green\"},{\"text\":\"flowing of time\",\"color\":\"blue\"},{\"text\":\" \",\"color\":\"blue\"},{\"text\":\"to \"},{\"text\":\"recharge \",\"color\":\"aqua\"},{\"text\":\"it.\"},{\"text\":\"\",\"color\":\"dark_purple\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"Right Click\",\"italic\":false,\"color\":\"dark_aqua\"},{\"text\":\" to set its \",\"color\":\"green\"},{\"text\":\"location \",\"color\":\"light_purple\"},{\"text\":\"and \",\"color\":\"green\"},{\"text\":\"to \",\"color\":\"green\"},{\"text\":\"teleport \",\"color\":\"dark_green\"},{\"text\":\"to its \",\"color\":\"green\"},{\"text\":\"set point\",\"color\":\"light_purple\"},{\"text\":\".\",\"color\":\"green\"},{\"text\":\"\",\"color\":\"dark_purple\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"\",\"italic\":false,\"color\":\"dark_purple\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"Location - \",\"italic\":false,\"color\":\"light_purple\"},{\"text\":\"Unbound\",\"color\":\"gray\"},{\"text\":\"\",\"color\":\"dark_purple\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"Charged - \",\"italic\":false,\"color\":\"dark_aqua\"},{\"text\":\"100%\",\"color\":\"blue\",\"bold\":true},{\"text\":\"\",\"color\":\"dark_purple\",\"bold\":false}]"));
      loreList.add(NbtString.of("[{\"text\":\"\",\"italic\":false,\"color\":\"dark_purple\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"Exotic \",\"italic\":false,\"color\":\"aqua\",\"bold\":true},{\"text\":\"Magic Item\",\"italic\":false,\"color\":\"dark_purple\",\"bold\":false}]"));
      display.put("Lore",loreList);
      tag.put("display",display);
      tag.put("Enchantments",enchants);
   
      setBookLore(makeLore());
      //setRecipe(makeRecipe());
      tag = addMagicNbt(tag);
      NbtCompound magicTag = tag.getCompound("arcananovum");
      NbtCompound locTag = new NbtCompound();
      locTag.putString("dim","unattuned");
      magicTag.putInt("heap",0);
      magicTag.put("location",locTag);
      prefNBT = tag;
      item.setNbt(prefNBT);
      prefItem = item;
   }
   
   @Override
   public void onTick(ServerWorld world, ServerPlayerEntity player, ItemStack item){
      NbtCompound itemNbt = item.getNbt();
      NbtCompound magicTag = itemNbt.getCompound("arcananovum");
      int heat = magicTag.getInt("heat");
      
      if(heat == 100){
         teleport(item,player);
         magicTag.putInt("heat",0);
         PLAYER_DATA.get(player).addXP(1000); // Add xp
      }else if(heat > 0){
         magicTag.putInt("heat",heat+1);
         ParticleEffectUtils.recallTeleportCharge(world,player.getPos());
      }else if(heat == -1){
         // Teleport was cancelled by damage
         ParticleEffectUtils.recallTeleportCancel(world,player.getPos());
         SoundUtils.playSound(player.getWorld(), player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.PLAYERS, 8,0.8f);
         magicTag.putInt("heat",0);
         setEnergy(item,maxEnergy/2);
      }
      
      if(world.getServer().getTicks() % 20 == 0){
         addEnergy(item, 1); // Recharge
         redoLore(item);
      }
   }
   
   @Override
   public ItemStack updateItem(ItemStack stack){
      NbtCompound itemNbt = stack.getNbt();
      NbtCompound magicTag = itemNbt.getCompound("arcananovum");
      NbtCompound locNbt = magicTag.getCompound("location");
      NbtCompound newTag = prefNBT.copy();
      newTag.getCompound("arcananovum").putString("UUID",magicTag.getString("UUID"));
      newTag.getCompound("arcananovum").putInt("energy",magicTag.getInt("energy"));
      newTag.getCompound("arcananovum").putInt("heat",magicTag.getInt("heat"));
      newTag.getCompound("arcananovum").put("location",locNbt.copy());
      stack.setNbt(newTag);
      redoLore(stack);
      return stack;
   }
   
   private void redoLore(ItemStack stack){
      NbtCompound itemNbt = stack.getNbt();
      NbtCompound magicNbt = itemNbt.getCompound("arcananovum");
      NbtCompound locNbt = magicNbt.getCompound("location");
      String dim = locNbt.getString("dim");
      
      NbtList loreList = itemNbt.getCompound("display").getList("Lore", NbtType.STRING);
      int charge = (getEnergy(stack)*100/maxEnergy);
      String charging = charge == 100 ? "Charged" : "Charging";
      if(!dim.equals("unattuned")){
         int x = (int) locNbt.getDouble("x");
         int y = (int) locNbt.getDouble("y");
         int z = (int) locNbt.getDouble("z");
         String dimColor;
         String dimensionName;
         String location;
         switch(dim){
            case "minecraft:overworld":
               dimColor = "green";
               dimensionName = "Overworld";
               break;
            case "minecraft:the_nether":
               dimColor = "red";
               dimensionName = "The Nether";
               break;
            case "minecraft:the_end":
               dimColor = "yellow";
               dimensionName = "The End";
               break;
            default:
               dimColor = "aqua";
               dimensionName = dim;
               break;
         }
         location = dimensionName + " ("+x+","+y+","+z+")";
         
         loreList.set(4,NbtString.of("[{\"text\":\"Location - \",\"italic\":false,\"color\":\"light_purple\"},{\"text\":\""+location+"\",\"color\":\""+dimColor+"\"},{\"text\":\"\",\"color\":\"dark_purple\"}]"));
         loreList.set(5,NbtString.of("[{\"text\":\""+charging+" - \",\"italic\":false,\"color\":\"dark_aqua\"},{\"text\":\""+charge+"%\",\"color\":\"blue\",\"bold\":true},{\"text\":\"\",\"color\":\"dark_purple\",\"bold\":false}]"));
      }else{
         loreList.set(4,NbtString.of("[{\"text\":\"Location - \",\"italic\":false,\"color\":\"light_purple\"},{\"text\":\"Unbound\",\"color\":\"gray\"},{\"text\":\"\",\"color\":\"dark_purple\"}]"));
         loreList.set(5,NbtString.of("[{\"text\":\""+charging+" - \",\"italic\":false,\"color\":\"dark_aqua\"},{\"text\":\""+charge+"%\",\"color\":\"blue\",\"bold\":true},{\"text\":\"\",\"color\":\"dark_purple\",\"bold\":false}]"));
      }
   }
   
   private void teleport(ItemStack item, ServerPlayerEntity player){
      NbtCompound itemNbt = item.getNbt();
      NbtCompound magicNbt = itemNbt.getCompound("arcananovum");
      NbtCompound locNbt = magicNbt.getCompound("location");
      String dim = locNbt.getString("dim");
      double x = locNbt.getDouble("x");
      double y = locNbt.getDouble("y");
      double z = locNbt.getDouble("z");
      float yaw = locNbt.getFloat("yaw");
      float pitch = locNbt.getFloat("pitch");
   
      ServerWorld to = player.getWorld();
      for (ServerWorld w : player.getServer().getWorlds()){
         if(w.getRegistryKey().getValue().toString().equals(dim)){
            to = w;
            break;
         }
      }
      
      player.teleport(to,x,y,z,yaw,pitch);
      setEnergy(item,0);
      SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_PORTAL_TRAVEL,1,2f);
      ParticleEffectUtils.recallTeleport(to,player.getPos());
   }
   
   @Override
   public boolean useItem(PlayerEntity playerEntity, World world, Hand hand){
      ItemStack item = playerEntity.getStackInHand(hand);
      if (playerEntity instanceof ServerPlayerEntity player){
         NbtCompound itemNbt = item.getNbt();
         NbtCompound magicNbt = itemNbt.getCompound("arcananovum");
         NbtCompound locNbt = magicNbt.getCompound("location");
         String dim = locNbt.getString("dim");
         
         if(dim.equals("unattuned")){
            locNbt.putString("dim",playerEntity.getWorld().getRegistryKey().getValue().toString());
            locNbt.putDouble("x",playerEntity.getPos().x);
            locNbt.putDouble("y",playerEntity.getPos().y);
            locNbt.putDouble("z",playerEntity.getPos().z);
            locNbt.putFloat("yaw",playerEntity.getYaw());
            locNbt.putFloat("pitch",playerEntity.getPitch());
            redoLore(item);
         }else{
            int curEnergy = getEnergy(item);
            if(curEnergy == maxEnergy){
               magicNbt.putInt("heat",1); // Starts the heat up process
               SoundUtils.playSound(player.getWorld(), player.getBlockPos(), SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 1,1);
            }else{
               playerEntity.sendMessage(new LiteralText("Pearl Recharging: "+(curEnergy*100/maxEnergy)+"%").formatted(Formatting.DARK_AQUA),true);
               SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,1,.5f);
            }
         }
      }
      return false;
   }
   
   @Override
   public boolean useItem(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult result){
      return false;
   }
   
   private MagicItemRecipe makeRecipe(){
      //TODO make recipe
      return null;
   }
   
   private List<String> makeLore(){
      //TODO make lore
      ArrayList<String> list = new ArrayList<>();
      list.add("{\"text\":\" TODO \"}");
      return list;
   }
}