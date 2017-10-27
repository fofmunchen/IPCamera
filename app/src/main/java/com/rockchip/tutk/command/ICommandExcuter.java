package com.rockchip.tutk.command;//
//  @ File Name : IMessageSender.java
//  @ Date : 2016/12/5
//  @ Author : qiujian
//
//


import com.rockchip.tutk.command.CommandBase;

public interface ICommandExcuter {
	public abstract String excute(CommandBase command);
}
