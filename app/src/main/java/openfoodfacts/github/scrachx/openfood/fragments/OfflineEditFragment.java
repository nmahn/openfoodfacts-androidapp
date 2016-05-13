package openfoodfacts.github.scrachx.openfood.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.loopj.android.http.RequestParams;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnItemLongClick;
import openfoodfacts.github.scrachx.openfood.R;
import openfoodfacts.github.scrachx.openfood.models.FoodUserClientUsage;
import openfoodfacts.github.scrachx.openfood.models.SaveItem;
import openfoodfacts.github.scrachx.openfood.models.SendProduct;
import openfoodfacts.github.scrachx.openfood.utils.Utils;
import openfoodfacts.github.scrachx.openfood.views.SaveProductOfflineActivity;
import openfoodfacts.github.scrachx.openfood.views.adapters.SaveListAdapter;

public class OfflineEditFragment extends BaseFragment {

    private ArrayList<SaveItem> saveItems;

    @Bind(R.id.listOfflineSave) ListView listView;
    @Bind(R.id.buttonSendAll) Button buttonSend;
    private String loginS, passS;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createView(inflater, container, R.layout.fragment_offline_edit);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final SharedPreferences settings = getContext().getSharedPreferences("login", 0);
        saveItems = new ArrayList<>();
        loginS = settings.getString("user", "");
        passS = settings.getString("pass", "");
        buttonSend.setEnabled(false);
    }

    @OnItemClick(R.id.listOfflineSave)
    protected void OnClickListOffline(int position) {
        SaveItem si = (SaveItem) listView.getItemAtPosition(position);
        SharedPreferences settings = getActivity().getSharedPreferences("temp", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("barcode", si.getBarcode());
        editor.apply();
        Intent intent = new Intent(getActivity(), SaveProductOfflineActivity.class);
        startActivity(intent);
    }

    @OnItemLongClick(R.id.listOfflineSave)
    protected boolean OnLongClickListOffline(int position) {
        final int lapos = position;
        new MaterialDialog.Builder(getActivity())
                .title(R.string.txtDialogsTitle)
                .content(R.string.txtDialogsContentDelete)
                .positiveText(R.string.txtYes)
                .negativeText(R.string.txtNo)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        String barcode = saveItems.get(lapos).getBarcode();
                        SendProduct.deleteAll(SendProduct.class, "barcode = ?", barcode);
                        final SaveListAdapter sl = (SaveListAdapter) listView.getAdapter();
                        saveItems.remove(lapos);
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                sl.notifyDataSetChanged();
                            }
                        });
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        // void implementation
                    }
                })
                .show();
        return true;
    }

    @OnClick(R.id.buttonSendAll)
    protected void onSendAllProducts() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.txtDialogsTitle)
                .content(R.string.txtDialogsContentSend)
                .positiveText(R.string.txtYes)
                .negativeText(R.string.txtNo)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        List<SendProduct> listSaveProduct = SendProduct.listAll(SendProduct.class);
                        FoodUserClientUsage user = new FoodUserClientUsage();
                        for (int i = 0; i < listSaveProduct.size(); i++) {
                            SendProduct sp = listSaveProduct.get(i);
                            if (sp.getBarcode().isEmpty() || sp.getImgupload_front().isEmpty() || sp.getImgupload_ingredients().isEmpty() || sp.getImgupload_nutrition().isEmpty()
                                    || sp.getStores().isEmpty() || sp.getWeight().isEmpty() || sp.getName().isEmpty()) {
                                // Do nothing
                            } else {
                                RequestParams params = new RequestParams();
                                params.put("code", sp.getBarcode());
                                if(!loginS.isEmpty() && !passS.isEmpty()) {
                                    params.put("user_id", loginS);
                                    params.put("password", passS);
                                }
                                params.put("product_name", sp.getName());
                                params.put("quantity", sp.getWeight() + " " + sp.getWeight_unit());
                                params.put("stores", sp.getStores());
                                params.put("comment", "added with the new Android app");

                                Utils.compressImage(sp.getImgupload_ingredients());
                                Utils.compressImage(sp.getImgupload_nutrition());
                                Utils.compressImage(sp.getImgupload_front());

                                user.postSaved(getActivity(), params, sp.getImgupload_front().replace(".png", "_small.png"), sp.getImgupload_ingredients().replace(".png", "_small.png"), sp.getImgupload_nutrition().replace(".png", "_small.png"), sp.getBarcode(), listView, i, saveItems);

                            }
                        }

                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        return;
                    }
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        new FillAdapter().execute(getActivity());
    }

    public class FillAdapter extends AsyncTask<Context, Void, Context> {

        @Override
        protected void onPreExecute() {
            saveItems.clear();
            List<SendProduct> listSaveProduct = SendProduct.listAll(SendProduct.class);
            if (listSaveProduct.size() == 0) {
                Toast.makeText(getActivity(), R.string.txtNoData, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), R.string.txtLoading, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected Context doInBackground(Context... ctx) {
            List<SendProduct> listSaveProduct = SendProduct.listAll(SendProduct.class);

            int imageIcon = R.drawable.ic_ok;
            for (int i = 0; i < listSaveProduct.size(); i++) {
                SendProduct sp = listSaveProduct.get(i);
                if (sp.getBarcode().isEmpty() || sp.getImgupload_front().isEmpty()
                        || sp.getStores().isEmpty() || sp.getWeight().isEmpty() || sp.getName().isEmpty()) {
                    imageIcon = R.drawable.ic_no;
                }
                Bitmap imgUrl = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(sp.getImgupload_front()), 200, 200, true);
                saveItems.add(new SaveItem(sp.getName(), imageIcon, imgUrl, sp.getBarcode()));
            }

            return ctx[0];
        }

        @Override
        protected void onPostExecute(Context ctx) {
            List<SendProduct> listSaveProduct = SendProduct.listAll(SendProduct.class);
            if (listSaveProduct.size() > 0) {
                SaveListAdapter adapter = new SaveListAdapter(ctx, saveItems);
                listView.setAdapter(adapter);
                buttonSend.setEnabled(true);
                for (SendProduct sp : listSaveProduct) {
                    if (sp.getBarcode().isEmpty() || sp.getImgupload_front().isEmpty() || sp.getImgupload_ingredients().isEmpty() || sp.getImgupload_nutrition().isEmpty()
                            || sp.getStores().isEmpty() || sp.getWeight().isEmpty() || sp.getName().isEmpty()) {
                        buttonSend.setEnabled(false);
                    }
                }
            } else {
                //Do nothing
            }
        }
    }
}
