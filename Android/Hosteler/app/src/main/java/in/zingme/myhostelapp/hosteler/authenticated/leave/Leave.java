package in.zingme.myhostelapp.hosteler.authenticated.leave;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import in.zingme.myhostelapp.hosteler.R;
import in.zingme.myhostelapp.hosteler.Views;
import in.zingme.myhostelapp.hosteler.appdata.AppData;
import in.zingme.myhostelapp.hosteler.appdata.datamodels.ActiveOuting;
import in.zingme.myhostelapp.hosteler.appdata.datamodels.LeaveRequest;
import in.zingme.myhostelapp.hosteler.appdata.room.AppDatabase;
import in.zingme.myhostelapp.hosteler.volley.MyVolley;

public class Leave extends Fragment {
    EditText reqPlace, reqPurpose, reqPhone, reqParent;
    TextView reqDateOut, reqDateIn, reqTimeOut;
    Spinner reqGoing;
    Button reqBtn;
    View rootView;
    View outingCard;
    View leaveRequestView;
    SwipeRefreshLayout swipeRefresh;
    SwipeRefreshLayout.OnRefreshListener onSwipeListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            MyVolley.getInstance(getActivity().getApplicationContext()).getRequestQueue().add(statusRequest);
        }
    };
    StringRequest cancelOuting = new StringRequest(Request.Method.POST, Views.id._181, new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {

            try {
                JSONObject jsonObject = new JSONObject(response);
                if (!jsonObject.getBoolean("error")) {
                    if (jsonObject.getString("access_token").equals(AppData.getInstance().getUserCard().getAccessToken())) {
                        if (jsonObject.getString("cancel_message").equals("cancelled")) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().deleteActiveOuting(AppData.getInstance().getActiveOuting());
                                    AppData.getInstance().setActiveOuting(null);
                                }
                            }).start();
                            outingCard.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(getActivity(), jsonObject.getString("cancel_message"), Toast.LENGTH_SHORT).show();
                            outingCard.animate().cancel();
                            outingCard.setVisibility(View.VISIBLE);
                            outingCard.setAlpha(1f);
                        }
                    } else {
                        Log.i("Volley-statusReq", "token-mismatch");
                        outingCard.animate().cancel();
                        outingCard.setVisibility(View.VISIBLE);
                        outingCard.setAlpha(1f);

                    }
                } else {
                    Log.e("Volley-statusReq", jsonObject.getString("errorMessage"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("Volley-statusReq", e.getMessage());
            }

        }
    }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Toast.makeText(getActivity(), "You cannot cancel now!", Toast.LENGTH_SHORT).show();
            outingCard.animate().cancel();
            outingCard.setVisibility(View.VISIBLE);
            outingCard.setAlpha(1f);

        }
    }) {

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap headers = new HashMap();
            headers.put("reqty", Views.id._081);
            return headers;
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            Map<String, String> params = new HashMap<String, String>();
            params.put("user_id", AppData.getInstance().getUserCard().getUserId());
            params.put("access_token", AppData.getInstance().getUserCard().getAccessToken());
            Log.e("Volley-authRequest", "POST" + params);
            return params;
        }
    };
    StringRequest statusRequest = new StringRequest(Request.Method.POST, Views.id._180, new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.e("Volley-authRequest", "RESPONSE" + response);
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (!jsonObject.getBoolean("error")) {
                    if (jsonObject.getString("access_token").equals(AppData.getInstance().getUserCard().getAccessToken())) {
                        if (!jsonObject.getString("outing_type").equals("null")) {
                            JSONObject outing = jsonObject.getJSONObject("outing");
                            ActiveOuting activeOuting;
                            activeOuting = new ActiveOuting(
                                    outing.getString("pass_id"),
                                    outing.getString("user_id"),
                                    outing.getString("otp"),
                                    outing.getString("hash"),
                                    outing.getString("phone"),
                                    outing.getString("place"),
                                    outing.getString("purpose"),
                                    null,
                                    outing.getString("hostel"),
                                    outing.getString("date_of_apply"),
                                    outing.getString("time_of_apply"),
                                    null, null, null, null, null,
                                    outing.getString("date_in"),
                                    outing.getString("time_out"),
                                    outing.getString("time_in"),
                                    null, null,
                                    outing.getString("sec_out_id"),
                                    outing.getString("sec_in_id"),
                                    outing.getString("status"),
                                    null,
                                    jsonObject.getString("bonus")
                            );
                            try {
                                activeOuting.setGoingWith(Objects.requireNonNull(getActivity()).getResources().getStringArray(R.array.going_with)[Integer.valueOf(outing.getString("going_with"))]);

                            } catch (NullPointerException e) {

                            }

                            if (jsonObject.getString("outing_type").equals("D")) {
                                activeOuting.setType("Dayout");
                            } else if (jsonObject.getString("outing_type").equals("L")) {
                                activeOuting.setWardenId(outing.getString("warden_id"));//
                                activeOuting.setRemark(outing.getString("remark"));//
                                activeOuting.setDateExpOut(outing.getString("date_exp_out"));//
                                activeOuting.setDateExpIn(outing.getString("date_exp_in"));
                                activeOuting.setTimeExpOut(outing.getString("time_exp_out"));
                                activeOuting.setParentNo(outing.getString("parent_no"));
                                activeOuting.setDateOut(outing.getString("date_out"));
                                activeOuting.setType("Leave");
                            }
                            AppData.getInstance().setActiveOuting(activeOuting);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().getActiveOutingCount() > 0) {
                                            AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().updateActiveOuting(AppData.getInstance().getActiveOuting());
                                        } else {
                                            AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().insertActive(AppData.getInstance().getActiveOuting());
                                        }
                                    } catch (NullPointerException e) {
                                        Log.e("EXCEPTION", "Leave-180");
                                    }
                                }
                            }).start();
                        } else {
                            AppData.getInstance().setActiveOuting(null);
                            try {
                                Toast.makeText(getActivity(), "No active outings", Toast.LENGTH_SHORT).show();

                            } catch (NullPointerException e) {
                            }
                        }

                    } else {
                        Log.i("Volley-statusReq", "token-mismatch");
                    }
                } else {
                    Log.e("Volley-statusReq", jsonObject.getString("errorMessage"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("Volley-statusReq", e.getMessage());
            }
            swipeRefresh.setRefreshing(false);
            showActiveOutingView();
        }
    }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            if (error != null) Log.e("Volley-statusReq", error.getMessage());
            swipeRefresh.setRefreshing(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().getActiveOutingCount() > 0) {
                            Log.i("Volley-statusReq", "error: " + "loading from DB-offline");
                            AppData.getInstance().setActiveOuting(AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().getActiveOuting().get(0));
                        } else {
                            Log.i("Volley-statusReq", "error: " + "No active outings");
                            AppData.getInstance().setActiveOuting(null);
                        }

                    } catch (NullPointerException e) {

                    }
                }
            }).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showActiveOutingView();
                }
            }, 500);
        }
    }) {
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap headers = new HashMap();
            headers.put("reqty", Views.id._080);
            return headers;
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            Map<String, String> params = new HashMap<String, String>();
            params.put("user_id", AppData.getInstance().getUserCard().getUserId());
            params.put("access_token", AppData.getInstance().getUserCard().getAccessToken());
            Log.e("Volley-authRequest", "POST" + params);
            return params;
        }
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_leave, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        outingCard = rootView.findViewById(R.id.outing_card);
        outingCard.setVisibility(View.GONE);
        leaveRequestView = rootView.findViewById(R.id.leave_request);
        leaveRequestView.setVisibility(View.GONE);
        swipeRefresh = rootView.findViewById(R.id.leave_swiperefresh);
        swipeRefresh.setOnRefreshListener(onSwipeListener);
        //onSwipeListener.onRefresh();
        loadLocal();
    }

    private void loadLocal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().getActiveOutingCount() > 0) {
                        Log.i("Volley-statusReq", "error: " + "loading from DB-offline");
                        AppData.getInstance().setActiveOuting(AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().getActiveOuting().get(0));
                    } else {
                        Log.i("Volley-statusReq", "error: " + "No active outings");
                        AppData.getInstance().setActiveOuting(null);
                    }

                } catch (NullPointerException e) {

                }
            }
        }).start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showActiveOutingView();
            }
        }, 500);
    }

    public void showActiveOutingView() throws NullPointerException {
        try {
            ((ProgressBar) rootView.findViewById(R.id.leave_progress)).setVisibility(View.INVISIBLE);
            if (AppData.getInstance().getActiveOuting() != null) {
                leaveRequestView.setVisibility(View.GONE);
                outingCard.setVisibility(View.VISIBLE);
                outingCard.setAlpha(0f);
                outingCard.animate().alphaBy(1f).setDuration(1000);
                ((TextView) outingCard.findViewById(R.id.card_active_type)).setText(AppData.getInstance().getActiveOuting().getType());
                if (AppData.getInstance().getActiveOuting().getType().equals("Dayout")) {
                    ((TextView) outingCard.findViewById(R.id.card_active_date)).setText(AppData.getInstance().getActiveOuting().getDateOfApply());
                } else {
                    ((TextView) outingCard.findViewById(R.id.card_active_date)).setText(AppData.getInstance().getActiveOuting().getDateExpOut());
                }
                ((TextView) outingCard.findViewById(R.id.card_active_otp)).setText(AppData.getInstance().getActiveOuting().getOtp());
                ((TextView) outingCard.findViewById(R.id.card_active_type)).setText(AppData.getInstance().getActiveOuting().getType());
                ((TextView) outingCard.findViewById(R.id.card_active_place)).setText(AppData.getInstance().getActiveOuting().getPlace());
                ((TextView) outingCard.findViewById(R.id.card_active_purpose)).setText(AppData.getInstance().getActiveOuting().getPurpose());
                if (AppData.getInstance().getActiveOuting().getStatus().equals("1")) {
                    ((TextView) outingCard.findViewById(R.id.card_active_status)).setText("REQUESTED");
                    ((View) outingCard.findViewById(R.id.card_active_head)).setBackground(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.shape_status_1));
                } else if (AppData.getInstance().getActiveOuting().getStatus().equals("2")) {
                    ((TextView) outingCard.findViewById(R.id.card_active_status)).setText("IN CAMPUS");
                    ((View) outingCard.findViewById(R.id.card_active_head)).setBackground(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.shape_status_2));
                } else if (AppData.getInstance().getActiveOuting().getStatus().equals("3")) {
                    ((TextView) outingCard.findViewById(R.id.card_active_status)).setText("OUTING");
                    (outingCard.findViewById(R.id.card_active_head)).setBackground(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.shape_status_3));
                }
                Log.i("View-timeout", AppData.getInstance().getActiveOuting().getTimeOut());
                if (AppData.getInstance().getActiveOuting().getTimeOut().equals("00:00:00")) {
                    Log.i("View-timeout", "equals");
                    ((TextView) outingCard.findViewById(R.id.card_active_timeout)).setText("Waiting");
                } else {
                    ((TextView) outingCard.findViewById(R.id.card_active_timeout)).setText(AppData.getInstance().getActiveOuting().getTimeOut());
                    Log.i("View-timeout", "NOT equals");
                }
                ((TextView) outingCard.findViewById(R.id.card_active_with)).setText(AppData.getInstance().getActiveOuting().getGoingWith());
                (outingCard.findViewById(R.id.card_active_cancel)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        outingCard.animate().alpha(0f).setDuration(4000);
                        MyVolley.getInstance(getActivity().getApplicationContext()).getRequestQueue().add(cancelOuting);
                    }
                });
                (outingCard.findViewById(R.id.card_active_viewcard)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Dialog card = new Dialog(getActivity(), R.style.AppTheme);
                        card.setContentView(R.layout.dialog_card);
                        card.setCancelable(true);
                        ((TextView) card.findViewById(R.id.dialog_card_name)).setText(AppData.getInstance().getUserCard().getName());
                        ((TextView) card.findViewById(R.id.dialog_card_hostel)).setText(AppData.getInstance().getUserCard().getHostel());
                        ((ImageView) card.findViewById(R.id.dialog_card_img)).setImageBitmap(BitmapFactory.decodeByteArray(Base64.decode(AppData.getInstance().getUserCard().getImg(), Base64.DEFAULT), 0, Base64.decode(AppData.getInstance().getUserCard().getImg(), Base64.DEFAULT).length));
                        if (AppData.getInstance().getActiveOuting() != null) {
                            ((TextView) card.findViewById(R.id.dialog_card_type)).setText(AppData.getInstance().getActiveOuting().getType());
                            if (AppData.getInstance().getActiveOuting().getStatus().equals("1")) {
                                ((View) card.findViewById(R.id.dialog_card_type)).setBackground(getActivity().getDrawable(R.drawable.shape_status_1));
                            } else if (AppData.getInstance().getActiveOuting().getStatus().equals("2")) {
                                ((View) card.findViewById(R.id.dialog_card_type)).setBackground(getActivity().getDrawable(R.drawable.shape_status_2));
                            } else if (AppData.getInstance().getActiveOuting().getStatus().equals("3")) {
                                ((View) card.findViewById(R.id.dialog_card_type)).setBackground(getActivity().getDrawable(R.drawable.shape_status_3));
                            }
                            ((TextView) card.findViewById(R.id.dialog_card_bonus)).setText("Bonus: " + AppData.getInstance().getActiveOuting().getBonus() + " mins");
                            ((TextView) card.findViewById(R.id.dialog_card_otp)).setText(AppData.getInstance().getActiveOuting().getOtp());
                            String qrContents = AppData.getInstance().getActiveOuting().getHash() + ":" + AppData.getInstance().getActiveOuting().getOtp() + ":" + AppData.getInstance().getActiveOuting().getPassId() + ":" + AppData.getInstance().getActiveOuting().getType().charAt(0) + ":" + AppData.getInstance().getActiveOuting().getUserId();
                            Log.i("QRCode", qrContents);
                            try {
                                BitMatrix bitMatrix = (new MultiFormatWriter()).encode(qrContents, BarcodeFormat.QR_CODE, 350, 350);
                                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                                Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                                ((ImageView) card.findViewById(R.id.dialog_card_qrcode)).setImageBitmap(bitmap);
                            } catch (WriterException e) {
                                e.printStackTrace();
                            }
                            ((TextView) card.findViewById(R.id.dialog_card_place)).setText(AppData.getInstance().getActiveOuting().getPlace());
                            ((TextView) card.findViewById(R.id.dialog_card_dateout)).setText(AppData.getInstance().getActiveOuting().getDateOut());
                            ((TextView) card.findViewById(R.id.dialog_card_timeout)).setText(AppData.getInstance().getActiveOuting().getTimeOut());
                            ((TextView) card.findViewById(R.id.dialog_card_secout)).setText(AppData.getInstance().getActiveOuting().getSecOutId());
                            ((TextView) card.findViewById(R.id.dialog_card_secin)).setText(AppData.getInstance().getActiveOuting().getSecInId());
                            ((TextView) card.findViewById(R.id.dialog_card_warid)).setText(AppData.getInstance().getActiveOuting().getWardenId());
                            ((TextView) card.findViewById(R.id.dialog_card_userid)).setText(AppData.getInstance().getActiveOuting().getUserId());
                            ((TextView) card.findViewById(R.id.dialog_card_phone)).setText(AppData.getInstance().getActiveOuting().getPhone());
                            ((TextView) card.findViewById(R.id.dialog_card_parent)).setText(AppData.getInstance().getActiveOuting().getParentNo());
                        } else {
                            ((View) card.findViewById(R.id.dialog_cardview_qr_otp)).setVisibility(View.GONE);
                            ((View) card.findViewById(R.id.dialog_cardview_outing_details)).setVisibility(View.GONE);
                        }
                        card.show();
                    }
                });
            } else {
                showRequestForm();

            }
        } catch (NullPointerException e) {

        }
    }

    private void showRequestForm() {
        leaveRequestView.setVisibility(View.VISIBLE);
        reqPlace = leaveRequestView.findViewById(R.id.leave_place);
        reqPurpose = leaveRequestView.findViewById(R.id.leave_purpose);
        reqGoing = leaveRequestView.findViewById(R.id.leave_goingwith);
        reqPhone = leaveRequestView.findViewById(R.id.leave_phone);
        reqDateOut = leaveRequestView.findViewById(R.id.leave_dateout);
        reqDateIn = leaveRequestView.findViewById(R.id.leave_datein);
        reqTimeOut = leaveRequestView.findViewById(R.id.leave_timeout);
        reqParent = leaveRequestView.findViewById(R.id.leave_parent);
        reqBtn = leaveRequestView.findViewById(R.id.leave_request_btn);
        reqBtn.setOnClickListener(requestOnClickListener);
        reqDateOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDatePicker(v.getId());
            }
        });
        reqDateIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDatePicker(v.getId());
            }
        });
        reqTimeOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchTimePicker(v.getId());
            }
        });

    }

    StringRequest leaveRequest = new StringRequest(Request.Method.POST, Views.id._183, new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.e("Volley-dayoutRequest", "RESPONSE" + response);
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (!jsonObject.getBoolean("error")) {
                    if (jsonObject.getString("access_token").equals(AppData.getInstance().getUserCard().getAccessToken())) {
                        if (jsonObject.getString("outing_type").equals("requested")) {
                            reqBtn.setBackground(getActivity().getDrawable(R.drawable.shape_button_primary));
                            reqBtn.setOnClickListener(requestOnClickListener);
                            onSwipeListener.onRefresh();
                            return;
                        } else {
                            AppData.getInstance().setDayoutRequest(null);
                            try {
                                Toast.makeText(getActivity(), "You have a pending Request", Toast.LENGTH_SHORT).show();
                            } catch (NullPointerException e) {
                            }
                        }

                    } else {
                        Log.i("Volley-dayoutRequest", "token-mismatch");
                    }
                } else {
                    Log.e("Volley-dayoutRequest", jsonObject.getString("errorMessage"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("Volley-dayoutRequest", e.getMessage());
            }
            swipeRefresh.setRefreshing(false);
            showActiveOutingView();
        }
    }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            if (error != null) Log.e("Volley-dayoutRequest", error.getMessage());
            swipeRefresh.setRefreshing(false);
            reqBtn.setBackground(getActivity().getDrawable(R.drawable.shape_button_primary));
            reqBtn.setOnClickListener(requestOnClickListener);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().getActiveOutingCount() > 0) {
                            Log.i("Volley-statusReq", "error: " + "loading from DB-offline");
                            AppData.getInstance().setActiveOuting(AppDatabase.getInstance(getActivity().getApplicationContext()).userCardDao().getActiveOuting().get(0));
                        } else {
                            Log.i("Volley-statusReq", "error: " + "No active outings");
                            AppData.getInstance().setActiveOuting(null);
                        }

                    } catch (NullPointerException e) {

                    }
                }
            }).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showActiveOutingView();
                }
            }, 500);

        }
    }) {
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap headers = new HashMap();
            headers.put("reqty", Views.id._083);
            return headers;
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            Map<String, String> params = new HashMap<String, String>();
            params.put("user_id", AppData.getInstance().getUserCard().getUserId());
            params.put("access_token", AppData.getInstance().getUserCard().getAccessToken());
            params.put("place", AppData.getInstance().getLeaveRequest().getPlace());
            params.put("purpose", AppData.getInstance().getLeaveRequest().getPurpose());
            params.put("going_with", AppData.getInstance().getLeaveRequest().getGoingWith());
            params.put("phone", AppData.getInstance().getLeaveRequest().getPhone());
            params.put("parent_no", AppData.getInstance().getLeaveRequest().getParent());
            params.put("date_exp_out", AppData.getInstance().getLeaveRequest().getDateExpOut());
            params.put("date_exp_in", AppData.getInstance().getLeaveRequest().getDateExpIn());
            params.put("time_exp_out", AppData.getInstance().getLeaveRequest().getTimeExpOut());
            Log.e("Volley-authRequest", "POST" + params);
            return params;
        }
    };

    public void launchDatePicker(int resId) {
        DatePicker datePicker = new DatePicker(resId);
        datePicker.show(getActivity().getFragmentManager(), "saddasa");
    }

    public void launchTimePicker(int resId) {
        TimePicker timePicker = new TimePicker(resId);
        timePicker.show(getActivity().getFragmentManager(), "saddasa");
    }

    View.OnClickListener requestOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean isPlaceValid, isPurposeValid, isGoingValid, isPhoneValid, isDateOutValid, isDateInValid, isTimeOutValid, isParentValid, aredatesValid;
            if (!(reqGoing.getSelectedItemId() > 0)) {
                reqGoing.setBackground(getActivity().getDrawable(R.drawable.input_error));
                isGoingValid = false;
            } else {
                isGoingValid = true;
                reqGoing.setBackground(getActivity().getDrawable(R.drawable.input_assert));
            }

            if (!(reqPlace.getText().toString().trim().length() > 3) || !reqPlace.getText().toString().matches("[a-zA-Z0-9 ]+")) {
                reqPlace.setBackground(getActivity().getDrawable(R.drawable.input_error));
                isPlaceValid = false;
            } else {
                isPlaceValid = true;
                reqPlace.setBackground(getActivity().getDrawable(R.drawable.input_assert));
            }

            if (!(reqPurpose.getText().toString().trim().length() > 3) || !reqPurpose.getText().toString().matches("[a-zA-Z0-9 ]+")) {
                reqPurpose.setBackground(getActivity().getDrawable(R.drawable.input_error));
                isPurposeValid = false;
            } else {
                isPurposeValid = true;
                reqPurpose.setBackground(getActivity().getDrawable(R.drawable.input_assert));
            }

            if (!(reqPhone.getText().toString().trim().length() > 3) || !reqPhone.getText().toString().matches("[0-9+]+")) {
                reqPhone.setBackground(getActivity().getDrawable(R.drawable.input_error));
                isPhoneValid = false;
            } else {
                isPhoneValid = true;
                reqPhone.setBackground(getActivity().getDrawable(R.drawable.input_assert));
            }

            if (!(reqParent.getText().toString().trim().length() > 3) || !reqParent.getText().toString().matches("[0-9+]+")) {
                reqParent.setBackground(getActivity().getDrawable(R.drawable.input_error));
                isParentValid = false;
            } else {
                isParentValid = true;
                reqParent.setBackground(getActivity().getDrawable(R.drawable.input_assert));
            }

            if (reqDateOut.getText().toString().equals("DATE OUT")) {
                reqDateOut.setBackground(getActivity().getDrawable(R.drawable.input_error));
                isDateOutValid = false;
            } else {
                isDateOutValid = true;
                reqDateOut.setBackground(getActivity().getDrawable(R.drawable.input_assert));
            }

            if (reqDateIn.getText().toString().equals("DATE IN")) {
                reqDateIn.setBackground(getActivity().getDrawable(R.drawable.input_error));
                isDateInValid = false;
            } else {
                isDateInValid = true;
                reqDateIn.setBackground(getActivity().getDrawable(R.drawable.input_assert));
            }
            if (reqTimeOut.getText().toString().equals("TIME OUT")) {
                reqTimeOut.setBackground(getActivity().getDrawable(R.drawable.input_error));
                isTimeOutValid = false;
            } else {
                isTimeOutValid = true;
                reqTimeOut.setBackground(getActivity().getDrawable(R.drawable.input_assert));
            }
            Date d1 = null, d2 = null;
            int difference = 0;
            try {
                d1 = new SimpleDateFormat("yyyy-MM-dd").parse(reqDateOut.getText().toString());
                d2 = new SimpleDateFormat("yyyy-MM-dd").parse(reqDateIn.getText().toString());
                difference = (int) TimeUnit.MILLISECONDS.toDays(d2.getTime() - d1.getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
            }
            Log.i("DATE", "" + difference);
            if (difference > 0) {
                ((TextView) leaveRequestView.findViewById(R.id.leave_day_count)).setVisibility(View.VISIBLE);
                ((TextView) leaveRequestView.findViewById(R.id.leave_day_count)).setText("You are outing for " + difference + " days!");
                aredatesValid = true;
            } else {
                aredatesValid = false;
            }

            if (isPlaceValid && isPurposeValid && isGoingValid && isPhoneValid && isDateOutValid && isDateInValid && isTimeOutValid && isParentValid && aredatesValid) {
                LeaveRequest lr = new LeaveRequest(reqPlace.getText().toString(), reqPurpose.getText().toString(), String.valueOf(reqGoing.getSelectedItemId()), reqDateOut.getText().toString(), reqDateIn.getText().toString(), reqTimeOut.getText().toString(), reqPhone.getText().toString(), reqParent.getText().toString());
                AppData.getInstance().setLeaveRequest(lr);
                ((ProgressBar) rootView.findViewById(R.id.leave_progress)).setVisibility(View.VISIBLE);
                MyVolley.getInstance(getActivity().getApplicationContext()).getRequestQueue().add(leaveRequest);

            }


        }
    };
}
