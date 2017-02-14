package bruce.sun.vr.mojing;

import android.util.Log;

import com.baofeng.mojing.MojingSDK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ManufacturerList {
    private static ManufacturerList mInstace;
    public String mClassName;
    public String mReleaseDate;
    public List<Manufacturer> mManufaturerList;

    private ManufacturerList() {
        mManufaturerList = new ArrayList<Manufacturer>();
    }

    public static ManufacturerList getInstance(String s) {
        if (mInstace == null) {
            mInstace = new ManufacturerList();
            PraseJsonManufactureList(s);
        }
        return mInstace;
    }

    public List<String> getAllDisplay() {
        List<String> displays = new ArrayList<String>();
        for (Manufacturer m : mManufaturerList) {
            displays.add(m.mDisplay);
            Log.d("MojingSDKDemo", "display" + m.mDisplay);
        }
        return displays;
    }

    private static void PraseJsonManufactureList(String strLanguageCodeByISO963) {
        try {
            JSONObject data = new JSONObject(
                    MojingSDK.GetManufacturerList(strLanguageCodeByISO963));
            JSONArray manufacturerList = data.getJSONArray("ManufacturerList");
            if (mInstace == null)
                mInstace = new ManufacturerList();
            mInstace.mClassName = data.getString("ClassName");
            mInstace.mReleaseDate = data.getString("ReleaseDate");
            for (int i = 0; i < manufacturerList.length(); i++) {
                JSONObject manufacturer = manufacturerList.getJSONObject(i);
                Manufacturer mf = new Manufacturer();
                mf.mDisplay = manufacturer.getString("Display");
                mf.mKEY = manufacturer.getString("KEY");
                JSONObject pro = new JSONObject(MojingSDK.GetProductList(
                        manufacturer.getString("KEY"), strLanguageCodeByISO963));
                JSONArray products = pro.getJSONArray("ProductList");
                for (int j = 0; j < products.length(); j++) {
                    JSONObject product = products.getJSONObject(j);
                    Product pt = new Product();
                    pt.mDisplay = product.getString("Display");
                    pt.mKey = product.getString("KEY");
                    JSONArray glasses = new JSONObject(MojingSDK.GetGlassList(
                            product.getString("KEY"), strLanguageCodeByISO963))
                            .getJSONArray("GlassList");
                    for (int k = 0; k < glasses.length(); k++) {
                        JSONObject glass = glasses.getJSONObject(k);
                        Glass gl = new Glass();
                        if (glass.has("Display")) {
                            gl.mDisplay = glass.getString("Display");
                        }
                        gl.mKey = glass.getString("KEY");

                        pt.mGlassList.add(gl);
                    } // end of for (int j = 0; j < products.length(); j++)
                    mf.mProductList.add(pt);
                } // end of for (int j = 0; j < products.length(); j++)
                mInstace.mManufaturerList.add(mf);
            } // end of for (int i = 0; i < manufacturerList.length(); i++)

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
