package com.peacecorps.malaria.ui.trip_reminder;

import android.content.Context;
import android.text.TextUtils;

import com.peacecorps.malaria.R;
import com.peacecorps.malaria.data.AppDataManager;
import com.peacecorps.malaria.data.db.DbHelper;
import com.peacecorps.malaria.ui.base.BasePresenter;
import com.peacecorps.malaria.ui.trip_reminder.PlanTripContract.PlanTripMvpPresenter;
import com.peacecorps.malaria.ui.trip_reminder.PlanTripContract.PlanTripMvpView;
import com.peacecorps.malaria.utils.ToastLogSnackBarUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Anamika Tripathi on 26/7/18.
 */
public class PlanTripPresenter<V extends PlanTripMvpView> extends BasePresenter<V> implements PlanTripMvpPresenter<V> {

    PlanTripPresenter(AppDataManager manager, Context context) {
        super(manager, context);
    }

    // get location history from db & calls createSelectLocationDialog if location is available else shows snack bar
    @Override
    public void setUpLocationDialog() {
        getDataManager().getLocation(new DbHelper.loadListStringCallBack() {
            @Override
            public void onDataLoaded(List<String> data) {
                if(data.size()>0) {
                    getView().createSelectLocationDialog(data);
                } else {
                    getView().showCustomSnackBar("No location history available");
                }

            }
        });
    }

    /**
     * @param hr   : hour selected in @TimePickerFragment (24 hour format)
     * @param mins : min selected in Fragment
     */
    @Override
    public String convertToTwelveHours(int hr, int mins) {
        String timeSet;
        int hour = 0;
        if (hr > 12) {
            hour = hr -12;
            timeSet = "PM";
        } else if (hr == 0) {
            hour += 12;
            timeSet = "AM";
        } else if (hr == 12) {
            timeSet = "PM";
            hour = hr;
        } else {
            timeSet = "AM";
            hour = hr;
        }

        String minutes;
        if (mins < 10) {
            minutes = getContext().getResources().getString(R.string.add_zero_beginning, mins);
        } else {
            minutes = String.valueOf(mins);
        }
        // Append the time to a stringBuilder
        return getContext().getResources().getString(R.string.time_picker, hour, minutes, timeSet);
    }

    // util function for testing text is empty of not
    @Override
    public boolean testIsEmpty(String text) {
        return TextUtils.isEmpty(text);
    }

    /**
     * @param s : date in string format, received by getText().toString() on edit Text
     * @return : Date object by parsing the parameter received
     */
    @Override
    public Date getDateObj(String s) {
        Date dateObj = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            dateObj = sdf.parse(s);
        } catch (ParseException e) {
            ToastLogSnackBarUtil.showErrorLog(s + "PlanTripPresenter: Parsing error in SimpleDateFormat");
        }
        return dateObj;
    }

    /**
     * @param date : date is converted to time using toTime()
     * @return : toTime() give long time in mili
     */
    @Override
    public long convertDateToTime(Date date) {
        return date.getTime();
    }

    /**
     * @param dateArr    : arrival date
     * @param dateDepart : departure date
     */
    @Override
    public void checkDateValidity(String dateArr, String dateDepart) {
        if (testIsEmpty(dateDepart)) {
            ToastLogSnackBarUtil.showToast(getContext(), "Select Departure date first");
        } else if (testIsEmpty(dateArr)) {
            ToastLogSnackBarUtil.showToast(getContext(), "Select Arrival date first");
        } else {
            String currDateString = new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date());
            Date currDate = getDateObj(currDateString);
            Date deptDate = getDateObj(dateDepart);
            Date arrDate = getDateObj(dateArr);

            if (currDate != null && deptDate != null && arrDate != null) {
                long arrLong = convertDateToTime(arrDate);
                long dptrLong = convertDateToTime(deptDate);
                long currLong = convertDateToTime(currDate);

                if (dptrLong < currLong) {
                    ToastLogSnackBarUtil.showToast(getContext(), getContext().getResources().getString(R.string.departuredate_currentdate));
                } else if (arrLong < dptrLong) {
                    ToastLogSnackBarUtil.showToast(getContext(), getContext().getResources().getString(R.string.arrivaldate_departuredate));
                } else if (arrLong >= dptrLong) {
                    // ask Fragment to start fragment, parameter is no of medicines calculated
                    getView().startSelectItemFragment(calculateNumOfMedicine(arrLong, dptrLong));
                }
            }
        }
    }

    /**
     * @param a : arrival time - converted in checkDateValidity()
     * @param d : departure time
     * @return : no of medicine to pack - displayed in select item fragment
     */
    @Override
    public long calculateNumOfMedicine(long a, long d) {
        int oneDay = 24 * 60 * 60 * 1000;

        return (a - d) / oneDay + 1;
    }

    /**
     * reminderMessage : saved in preferences
     * location selected/added : added in database (if old location selected, time: gets increased (no of time trip planned)
     */
    @Override
    public void saveTripDetails() {
        String reminderMessage = "Trip to " + getView().getLocationText() +
                " is scheduled for " + getView().getDepartureTimeText()
                + ".\n" + "Stay safe, don't forget to take your pills.";
        getDataManager().setReminderMessageForTrip(reminderMessage);
        getDataManager().insertLocation(getView().getLocationText());
        ToastLogSnackBarUtil.showToast(getContext(), "Trip Saved");
    }

    /**
     * validates all edit text, if all returns true, calls saveTripDetails()
     */
    @Override
    public void validationGenerateButton() {
        if (!getView().validateTripLocation()) {
            return;
        }

        if (!getView().validateDepartureDate()) {
            return;
        }

        if (!getView().validateArrivalDate()) {
            return;
        }

        if (!getView().validateSelectItem()) {
            return;
        }

        if (!getView().validateReminderTime()) {
            return;
        }

        saveTripDetails();
    }
}
