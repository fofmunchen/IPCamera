package com.rockchip.tutk.command;

import android.util.Log;

import com.rockchip.tutk.TUTKSession;
import com.rockchip.tutk.command.CommandBase;
import com.rockchip.tutk.command.ICommandExcuter;

/**
 * Created by qiujian on 2017/1/3.
 */

public class CommandExcuterImp implements ICommandExcuter {

    TUTKSession mSession;
    private String TAG = "CommandExcuterImp";

    public CommandExcuterImp(TUTKSession mSession) {
        this.mSession = mSession;
    }

    @Override
    public String excute(CommandBase command) {
        Log.d(TAG, "execute:" + command.Json());
        String read = "";
        int write = mSession.write(command.Json());
        Log.d(TAG, "write:" + write);
        if (write > 0) {
            read = mSession.read();
        }
        return read;
    }
}
