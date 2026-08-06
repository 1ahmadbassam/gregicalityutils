package top.ahmadb.gregicalityutils.extrautils2;

import com.rwtema.extrautils2.itemhandler.SingleStackHandlerUpgrades;
import com.rwtema.extrautils2.utils.datastructures.NBTSerializable;

public interface IAnalogCrafterExtensions {
    SingleStackHandlerUpgrades gcu_getUpgrades();
    NBTSerializable.NBTBoolean gcu_getLimitToOne();
    NBTSerializable.NBTBoolean gcu_getStrictSlots();
}