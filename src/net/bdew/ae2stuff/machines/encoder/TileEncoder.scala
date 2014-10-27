package net.bdew.ae2stuff.machines.encoder

import appeng.api.AEApi
import appeng.api.networking.{GridNotification, IGrid}
import net.bdew.ae2stuff.grid.GridTile
import net.bdew.lib.Misc
import net.bdew.lib.tile.TileExtended
import net.bdew.lib.tile.inventory.{BreakableInventoryTile, PersistentInventoryTile, SidedInventory}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}

class TileEncoder extends TileExtended with GridTile with PersistentInventoryTile with BreakableInventoryTile with SidedInventory {
  override def getSizeInventory = 12

  object slots {
    val recipe = (0 until 9).toArray
    val result = 9
    val patterns = 10
    val encoded = 11
  }

  lazy val blankPattern = AEApi.instance().materials().materialBlankPattern
  lazy val encodedPattern = AEApi.instance().items().itemEncodedPattern.item()

  def getRecipe = slots.recipe map getStackInSlot
  def getResult = getStackInSlot(slots.result)

  override def markDirty() {
    if (!worldObj.isRemote)
      inv(slots.encoded) = encodePattern() // direct to prevent an infinite recursion
    super.markDirty()
  }

  def encodePattern(): ItemStack = {
    if (getResult == null || !getRecipe.exists(_ != null) || getStackInSlot(slots.patterns) == null)
      return null

    val newStack = new ItemStack(encodedPattern)

    val tag = new NBTTagCompound
    val inList = new NBTTagList
    val outList = new NBTTagList

    for (s <- getRecipe)
      if (s != null)
        inList.appendTag(Misc.applyMutator(s.writeToNBT, new NBTTagCompound))
      else
        inList.appendTag(new NBTTagCompound)

    outList.appendTag(Misc.applyMutator(getResult.writeToNBT, new NBTTagCompound))

    tag.setTag("in", inList)
    tag.setTag("out", outList)
    tag.setBoolean("crafting", true)

    newStack.setTagCompound(tag)
    newStack
  }

  // Inventory stuff

  allowSided = true
  override def canExtractItem(slot: Int, stack: ItemStack, side: Int) = false
  override def getAccessibleSlotsFromSide(side: Int) = Array(slots.patterns)
  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    slot == slots.patterns && blankPattern.sameAsStack(stack)

  override def getMachineRepresentation = new ItemStack(BlockEncoder)

  override def onGridNotification(p1: GridNotification) = println("Grid notification: " + p1)
  override def setNetworkStatus(p1: IGrid, p2: Int) = println("Set net status: " + p1 + ", " + p2)
  override def gridChanged() = println("Grid changed")
}