package com.rockchip.tutk.command;

import com.rockchip.tutk.EncoderParameter;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 2017/3/21.
 */

public class SetEncoderParameters extends CommandBase implements JsonInterface{

    EncoderParameter mEncoderParameter;

    public SetEncoderParameters(EncoderParameter encoderParameter) {
        this.mEncoderParameter = encoderParameter;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "setEncoderParameters");
            jsonObject.put("type","Settings");
            jsonObject.put("frame_rate",Integer.parseInt(mEncoderParameter.getFrame_rate()));
            jsonObject.put("level",Integer.parseInt(mEncoderParameter.getLevel()));
            jsonObject.put("gop_size",Integer.parseInt(mEncoderParameter.getGop_size()));
            jsonObject.put("profile",Integer.parseInt(mEncoderParameter.getProfile()));
            jsonObject.put("quality",Integer.parseInt(mEncoderParameter.getQuality()));
            jsonObject.put("qp_init",Integer.parseInt(mEncoderParameter.getQp_init()));
            jsonObject.put("qp_min",Integer.parseInt(mEncoderParameter.getQp_min()));
            jsonObject.put("qp_max",Integer.parseInt(mEncoderParameter.getQp_max()));
            jsonObject.put("qp_step",Integer.parseInt(mEncoderParameter.getQp_step()));
            jsonObject.put("rc_mode",Integer.parseInt(mEncoderParameter.getRc_mode()));
            jsonObject.put("width",Integer.parseInt(mEncoderParameter.getWidth()));
            jsonObject.put("height",Integer.parseInt(mEncoderParameter.getHeight()));
            jsonObject.put("bit_rate",Integer.parseInt(mEncoderParameter.getBit_rate()));
            jsonObject.put("value",Integer.parseInt(mEncoderParameter.getChannel()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
