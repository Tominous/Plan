package main.java.com.djrapitops.plan.utilities.html.tables;

import main.java.com.djrapitops.plan.data.UserInfo;
import main.java.com.djrapitops.plan.utilities.MiscUtils;

import java.util.List;

/**
 * @author Rsl1122
 */
// TODO Rewrite!
public class PlayersTableCreator {

    /**
     * Constructor used to hide the public constructor
     */
    private PlayersTableCreator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param data The list of the {@link UserInfo} Objects from which the players table should be created
     * @return The created players table
     */
    public static String createSortablePlayersTable(List<UserInfo> data) {
        StringBuilder html = new StringBuilder();

        long now = MiscUtils.getTime();

        int i = 0;
        for (UserInfo uData : data) {
            if (i >= 750) {
                break;
            }

            try {
//                boolean isBanned = uData.isBanned();
//                boolean isUnknown = uData.getLoginTimes() == 1;
//                boolean isActive = AnalysisUtils.isActive(now, uData.getLastPlayed(), uData.getPlayTime(), uData.getLoginTimes());
//
//                String activityString = getActivityString(isBanned, isUnknown, isActive);
//
//                html.append(Html.TABLELINE_PLAYERS.parse(
//                        Html.LINK.parse(HtmlUtils.getRelativeInspectUrl(uData.getName()), uData.getName()),
//                        activityString,
//                        String.valueOf(uData.getPlayTime()), FormatUtils.formatTimeAmount(uData.getPlayTime()),
//                        String.valueOf(uData.getLoginTimes()),
//                        String.valueOf(uData.getRegistered()), FormatUtils.formatTimeStampYear(uData.getRegistered()),
//                        String.valueOf(uData.getLastPlayed()), FormatUtils.formatTimeStamp(uData.getLastPlayed()),
//                        String.valueOf(uData.getGeolocations()) //TODO get last Geoloc
//                ));
            } catch (NullPointerException ignored) {
            }

            i++;
        }

        return html.toString();
    }

    private static String getActivityString(boolean isBanned, boolean isUnknown, boolean isActive) {
        if (isBanned) {
            return "Banned";
        }

        if (isUnknown) {
            return "Unknown";
        }

        return isActive ? "Active" : "Inactive";
    }
}