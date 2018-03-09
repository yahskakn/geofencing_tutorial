package com.tinmegali.mylocation;

import java.util.List;

/**
 * Created by akankanh on 3/8/18.
 */

public interface FetchReply {
    public void onReply(boolean success, List<OccupancyDatabase> data);
}
