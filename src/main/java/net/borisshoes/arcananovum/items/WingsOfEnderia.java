package net.borisshoes.arcananovum.items;

import net.borisshoes.arcananovum.core.EnergyItem;
import net.borisshoes.arcananovum.core.polymer.MagicPolymerArmorItem;
import net.borisshoes.arcananovum.utils.MagicRarity;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class WingsOfEnderia extends EnergyItem {
   public WingsOfEnderia(){
      id = "wings_of_enderia";
      name = "Armored Wings of Enderia";
      rarity = MagicRarity.MYTHICAL;
      categories = new ArcaneTome.TomeFilter[]{ArcaneTome.TomeFilter.MYTHICAL, ArcaneTome.TomeFilter.EQUIPMENT};
      itemVersion = 1;
      vanillaItem = Items.ELYTRA;
      item = new WingsOfEnderiaItem(new FabricItemSettings().maxCount(1).fireproof());
      
      ItemStack stack = new ItemStack(item);
      NbtCompound tag = stack.getOrCreateNbt();
      NbtCompound display = new NbtCompound();
      NbtList loreList = new NbtList();
      NbtList enchants = new NbtList();
      NbtList attributes = new NbtList();
      NbtCompound prot = new NbtCompound();
      prot.putString("id","protection");
      prot.putInt("lvl",4);
      enchants.add(prot);
      display.putString("Name","[{\"text\":\"Armored Wings of Enderia\",\"italic\":false,\"bold\":true,\"color\":\"gray\"}]");
      loreList.add(NbtString.of("[{\"text\":\"These \",\"italic\":false,\"color\":\"white\"},{\"text\":\"Gentle Wings\",\"color\":\"gray\"},{\"text\":\" will shield you from the \"},{\"text\":\"dangers \",\"color\":\"yellow\"},{\"text\":\"of the land.\",\"color\":\"white\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"These \",\"italic\":false,\"color\":\"white\"},{\"text\":\"Wings \",\"color\":\"gray\"},{\"text\":\"act as a \"},{\"text\":\"Netherite Chestplate\",\"color\":\"dark_red\"},{\"text\":\" with \"},{\"text\":\"Protection IV\",\"color\":\"aqua\"},{\"text\":\".\",\"color\":\"white\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"They store \",\"italic\":false,\"color\":\"white\"},{\"text\":\"energy \",\"color\":\"yellow\"},{\"text\":\"as you fly to \"},{\"text\":\"cushion impacts\",\"color\":\"gray\"},{\"text\":\" and are \"},{\"text\":\"unbreakable\",\"color\":\"blue\"},{\"text\":\".\"},{\"text\":\"\",\"color\":\"dark_purple\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"\",\"italic\":false,\"color\":\"dark_purple\"}]"));
      loreList.add(NbtString.of("[{\"text\":\"Mythical \",\"italic\":false,\"color\":\"light_purple\",\"bold\":true},{\"text\":\"Magic Item\",\"italic\":false,\"color\":\"dark_purple\",\"bold\":false}]"));
      display.put("Lore",loreList);
      NbtCompound kbRes = new NbtCompound();
      kbRes.putDouble("Amount",0.1);
      kbRes.putString("AttributeName","generic.knockback_resistance");
      kbRes.putString("Name","generic.knockback_resistance");
      kbRes.putString("Slot","chest");
      kbRes.putIntArray("UUID", new int[]{-122030, 92433, 23139, -184866});
      attributes.add(kbRes);
      NbtCompound toughness = new NbtCompound();
      toughness.putInt("Amount",3);
      toughness.putString("AttributeName","generic.armor_toughness");
      toughness.putString("Name","generic.armor_toughness");
      toughness.putString("Slot","chest");
      toughness.putIntArray("UUID", new int[]{-122030, 92533, 23139, -185066});
      attributes.add(toughness);
      NbtCompound armor = new NbtCompound();
      armor.putInt("Amount",8);
      armor.putString("AttributeName","generic.armor");
      armor.putString("Name","generic.armor");
      armor.putString("Slot","chest");
      armor.putIntArray("UUID", new int[]{-122030, 92633, 23139, -185266});
      attributes.add(armor);
      tag.put("display",display);
      tag.put("Enchantments",enchants);
      tag.put("AttributeModifiers",attributes);
      tag.putInt("HideFlags", 255);
      tag.putInt("Unbreakable",1);
   
      setBookLore(makeLore());
      prefNBT = addMagicNbt(tag);
      stack.setNbt(prefNBT);
      prefItem = stack;
   }
   
   @Override
   public int getMaxEnergy(ItemStack item){
      return 10000; // Store up to 100 points of dmg mitigation at 5 seconds of flight per damage point stored aka 100 ticks/energy per 1 dmg point
   }
   
   @Override
   public ItemStack updateItem(ItemStack stack, MinecraftServer server){
      NbtCompound itemNbt = stack.getNbt();
      NbtList enchants = itemNbt.getList("Enchantments", NbtElement.COMPOUND_TYPE);
      NbtCompound newTag = super.updateItem(stack,server).getNbt();
      if(enchants != null) newTag.put("Enchantments", enchants);
      stack.setNbt(newTag);
      return stack;
   }
   
   private List<String> makeLore(){
      ArrayList<String> list = new ArrayList<>();
      list.add("{\"text\":\"     Armored Wings \\n       of Enderia\\n\\nRarity: Mythical\\n\\nThe first discovered Mythical Item. They offer unsurmounted protection equivalent to the strongest of armor.\\n\\nThe Wings themselves are hardened to the \"}");
      list.add("{\"text\":\"     Armored Wings \\n       of Enderia\\n\\npoint of being impervious to structural damage.\\n\\nAs study of them furthers a new ability has been discovered.\\nThe Wings collect and store kinetic energy when in flight that can be re-emitted when\"}");
      list.add("{\"text\":\"     Armored Wings \\n       of Enderia\\n\\nthe wearer suffers a large impact.\\n\\nThis effect seems to negate up to half of all fall damage and kinetic damage taken as long as the Wings have stored enough energy.\"}");
      return list;
   }
   
   public class WingsOfEnderiaItem extends MagicPolymerArmorItem {
      public WingsOfEnderiaItem(Settings settings){
         super(getThis(),ArmorMaterials.NETHERITE,Type.CHESTPLATE,settings);
      }
      
      @Override
      public ItemStack getDefaultStack(){
         return prefItem;
      }
   }
}