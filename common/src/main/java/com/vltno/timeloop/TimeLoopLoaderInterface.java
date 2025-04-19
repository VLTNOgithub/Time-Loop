package com.vltno.timeloop;

import net.minecraft.server.level.ServerPlayer;
import net.mt1006.mocap.network.MocapPacketC2S;
import net.mt1006.mocap.network.MocapPacketS2C;

public interface TimeLoopLoaderInterface
{
	String getLoaderName();
	String getModVersion();
}
