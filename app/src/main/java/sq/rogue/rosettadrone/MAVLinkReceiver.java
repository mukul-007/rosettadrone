package sq.rogue.rosettadrone;

import android.util.Log;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_mission_ack;
import com.MAVLink.common.msg_mission_count;
import com.MAVLink.common.msg_mission_item;
import com.MAVLink.common.msg_mission_request;
import com.MAVLink.common.msg_param_request_read;
import com.MAVLink.common.msg_param_set;
import com.MAVLink.common.msg_set_mode;
import com.MAVLink.enums.MAV_RESULT;

import java.util.ArrayList;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;

import static com.MAVLink.common.msg_command_int.MAVLINK_MSG_ID_COMMAND_INT;
import static com.MAVLink.common.msg_command_long.MAVLINK_MSG_ID_COMMAND_LONG;
import static com.MAVLink.common.msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT;
import static com.MAVLink.common.msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK;
import static com.MAVLink.common.msg_mission_clear_all.MAVLINK_MSG_ID_MISSION_CLEAR_ALL;
import static com.MAVLink.common.msg_mission_count.MAVLINK_MSG_ID_MISSION_COUNT;
import static com.MAVLink.common.msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM;
import static com.MAVLink.common.msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST;
import static com.MAVLink.common.msg_mission_request_list.MAVLINK_MSG_ID_MISSION_REQUEST_LIST;
import static com.MAVLink.common.msg_mission_request_partial_list.MAVLINK_MSG_ID_MISSION_REQUEST_PARTIAL_LIST;
import static com.MAVLink.common.msg_mission_set_current.MAVLINK_MSG_ID_MISSION_SET_CURRENT;
import static com.MAVLink.common.msg_param_request_list.MAVLINK_MSG_ID_PARAM_REQUEST_LIST;
import static com.MAVLink.common.msg_param_request_read.MAVLINK_MSG_ID_PARAM_REQUEST_READ;
import static com.MAVLink.common.msg_param_set.MAVLINK_MSG_ID_PARAM_SET;
import static com.MAVLink.common.msg_set_mode.MAVLINK_MSG_ID_SET_MODE;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_DO_DIGICAM_CONTROL;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_DO_SET_HOME;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_DO_SET_MODE;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_GET_HOME_POSITION;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_NAV_LAND;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_NAV_LOITER_UNLIM;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_NAV_TAKEOFF;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_REQUEST_AUTOPILOT_CAPABILITIES;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_VIDEO_START_CAPTURE;
import static com.MAVLink.enums.MAV_CMD.MAV_CMD_VIDEO_STOP_CAPTURE;
import static com.MAVLink.enums.MAV_MISSION_TYPE.MAV_MISSION_TYPE_MISSION;
import static dji.common.flightcontroller.FlightControlState.ATTI;


public class MAVLinkReceiver {
    private final String TAG = "RosettaDrone";

    DroneModel mModel;
    private long mTimeStampLastGCSHeartbeat = 0;

    private int mNumGCSWaypoints = 0;
    private int wpState = 0;
    private final int WP_STATE_INACTIVE = 0;
    private final int WP_STATE_REQ_COUNT = 1;
    private final int WP_STATE_REQ_WP = 2;
    private MainActivity parent;
    private WaypointMission.Builder mBuilder;
    private List<Waypoint> mWaypointList;

    public MAVLinkReceiver(MainActivity parent, DroneModel model) {

        this.parent = parent;
        this.mModel = model;

    }

    public void process(MAVLinkMessage msg) {
        switch (msg.msgid) {
            case MAVLINK_MSG_ID_HEARTBEAT:
                this.mTimeStampLastGCSHeartbeat = System.currentTimeMillis();
                break;

            case MAVLINK_MSG_ID_COMMAND_LONG:
                msg_command_long msg_cmd = (msg_command_long) msg;
                switch (msg_cmd.command) {
                    case MAV_CMD_COMPONENT_ARM_DISARM:
                        if (msg_cmd.param1 == 1) {
                            parent.logMessageDJI("Command: arm motors");
                            mModel.send_command_ack(MAV_CMD_COMPONENT_ARM_DISARM, MAV_RESULT.MAV_RESULT_ACCEPTED);
                            mModel.armMotors();
                        }
                        else
                            mModel.disarmMotors();

                        break;
                    case MAV_CMD_DO_SET_MODE:
                        changeFlightMode((int)msg_cmd.param1);
                        break;
                    case MAV_CMD_NAV_LOITER_UNLIM:
                        mModel.set_flight_mode(ATTI);
                        break;
                    case MAV_CMD_NAV_TAKEOFF:
                        mModel.do_takeoff();
                        break;
                    case MAV_CMD_NAV_LAND:
                        mModel.do_land();
                        break;
                    case MAV_CMD_DO_SET_HOME:
                        // TODO;
                        break;
                    case MAV_CMD_NAV_RETURN_TO_LAUNCH:
                        mModel.do_go_home();
                        mModel.send_command_ack(MAV_CMD_NAV_RETURN_TO_LAUNCH, MAV_RESULT.MAV_RESULT_ACCEPTED);
                        break;
                    case MAV_CMD_GET_HOME_POSITION:
                        mModel.send_home_position();
                        break;
                    case MAV_CMD_REQUEST_AUTOPILOT_CAPABILITIES:
                        mModel.send_autopilot_version();
                        break;
                    case MAV_CMD_VIDEO_START_CAPTURE:
                        break;
                    case MAV_CMD_VIDEO_STOP_CAPTURE:
                        break;
                    case MAV_CMD_DO_DIGICAM_CONTROL:
                        // DEPRECATED but still used by QGC
                        mModel.takePhoto();
                        break;
                }
                break;

            case MAVLINK_MSG_ID_COMMAND_INT:
                // TODO I don't understand what this message is, but ArduCopter handles it.
                // See ArduCopter/GCS_Mavlink.cpp
                break;

            case MAVLINK_MSG_ID_SET_MODE:
                msg_set_mode msg_set_mode = (msg_set_mode) msg;
                parent.logMessageDJI("MAVLINK_MSG_ID_SET_MODE: " + msg_set_mode.custom_mode);
                changeFlightMode((int)msg_set_mode.custom_mode);
                break;

            /**************************************************************
             * These messages are used when GCS requests params from MAV  *
             **************************************************************/

            case MAVLINK_MSG_ID_PARAM_REQUEST_LIST:
                mModel.send_all_params();
                break;

            case MAVLINK_MSG_ID_PARAM_REQUEST_READ:
                msg_param_request_read msg_param = (msg_param_request_read) msg;
                //String paramStr = msg_param.getParam_Id();
                //parent.logMessageFromGCS("***" + paramStr);
                mModel.send_param(msg_param.param_index);
                // TODO I am not able to convert the param_id bytearray into String
//                for(int i = 0; i < mModel.getParams().size(); i++)
//                    if(mModel.getParams().get(i).getParamName().equals(msg_param.getParam_Id())) {
//                        mModel.send_param(i);
//                        break;
//                    }
                Log.d(TAG, "Request to read param that doesn't exist");
                break;

            case MAVLINK_MSG_ID_PARAM_SET:
                msg_param_set msg_param2 = (msg_param_set) msg;
                MAVParam param = new MAVParam(msg_param2.getParam_Id(),
                        msg_param2.param_value,
                        msg_param2.param_type);
                mModel.changeParam(param);
                break;

            /**************************************************************
             * These messages are used when GCS downloads mission from MAV *
             **************************************************************/

            case MAVLINK_MSG_ID_MISSION_REQUEST_LIST:
                parent.logMessageDJI("MSN: RD received mission_request_list from GCS");
                mModel.send_mission_count();
                break;

            case MAVLINK_MSG_ID_MISSION_REQUEST_PARTIAL_LIST:
                // TODO
                break;

            case MAVLINK_MSG_ID_MISSION_REQUEST:
                msg_mission_request msg_request = new msg_mission_request();
                parent.logMessageDJI("MSN: RD received mission_request from GCS: " + String.valueOf(msg_request.seq));
                mModel.send_mission_item(msg_request.seq);
                break;

            case MAVLINK_MSG_ID_MISSION_ACK:
                msg_mission_ack msg_ack = new msg_mission_ack();
                parent.logMessageDJI("MSN: RD received mission_ack from GCS");
                if (msg_ack.type == MAV_MISSION_TYPE_MISSION) {
                    parent.logMessageDJI("MSN: ack success");
                    // TODO success
                } else {
                    parent.logMessageDJI("MSN: ack fail");
                    // TODO fail
                }
                break;

            /**************************************************************
             * These messages are used when GCS uploads a mission to MAV  *
             **************************************************************/

            case MAVLINK_MSG_ID_MISSION_COUNT:
                generateNewMission();
                //mModel.getMissionControl().getWaypointMissionOperator().getLoadedMission().getWaypointList().clear();
                msg_mission_count msg_count = (msg_mission_count) msg;
                mNumGCSWaypoints = msg_count.count;
                parent.logMessageDJI("Num waypoints from GCS: " + String.valueOf(mNumGCSWaypoints));
                wpState = WP_STATE_REQ_WP;
                mModel.request_mission_item(0);
                break;

            case MAVLINK_MSG_ID_MISSION_ITEM:
                msg_mission_item msg_item = (msg_mission_item) msg;
                parent.logMessageDJI("MSN: Received mission_item from GCS: " + String.valueOf(msg_item.seq));
                parent.logMessageDJI("   " + String.valueOf(msg_item.x) + ", " + String.valueOf(msg_item.y));
                Waypoint wp = new Waypoint(msg_item.x, msg_item.y, msg_item.z); // TODO check altitude conversion
                mWaypointList.add(wp);

                // We are done fetching a complete mission from the GCS...
                if (msg_item.seq == mNumGCSWaypoints - 1) {
                    parent.logMessageDJI("All waypoints received from GCS");
                    wpState = WP_STATE_INACTIVE;
                    finalizeNewMission();
                    mModel.send_mission_ack();
                } else {
                    mModel.request_mission_item((msg_item.seq + 1));
                }
                break;

            /**************************************************************
             * These messages from GCS direct a mission-related action    *
             **************************************************************/

            case MAVLINK_MSG_ID_MISSION_SET_CURRENT:
                parent.logMessageDJI("MSN: received set_current from GCS");
                // TODO
                break;

            case MAVLINK_MSG_ID_MISSION_CLEAR_ALL:
                parent.logMessageDJI("MSN: received clear_all from GCS");
                mModel.getWaypointMissionOperator().getLoadedMission().getWaypointList().clear();
                break;
        }

    }

    public long getTimestampLastGCSHeartbeat() {
        return mTimeStampLastGCSHeartbeat;
    }

    private void changeFlightMode(int flightMode) {
        mModel.setGCSCommandedMode(flightMode);

        if (flightMode == ArduCopterFlightModes.AUTO)
            if(mModel.getWaypointMissionOperator().getCurrentState() == WaypointMissionState.EXECUTION_PAUSED) {
                parent.logMessageDJI("Command: resume mission");
                mModel.resumeWaypointMission();
            }
            else {
                parent.logMessageDJI("Command: start mission");
                mModel.startWaypointMission();
            }
        else if(flightMode == ArduCopterFlightModes.BRAKE) {
            mModel.pauseWaypointMission();
            mModel.setGCSCommandedMode(flightMode);
        }

        else if (flightMode == ArduCopterFlightModes.RTL)
            mModel.do_go_home();
        else if (flightMode == ArduCopterFlightModes.LAND)
            mModel.do_land();

        if (mModel.getWaypointMissionOperator().getCurrentState() == WaypointMissionState.EXECUTING && flightMode != ArduCopterFlightModes.AUTO)
            parent.logMessageDJI("Command: pause mission");
            mModel.pauseWaypointMission();

        mModel.send_command_ack(MAV_CMD_DO_SET_MODE, MAV_RESULT.MAV_RESULT_ACCEPTED);

    }

    protected void generateNewMission() {
        mBuilder = new WaypointMission.Builder();
        mBuilder.autoFlightSpeed(5f);
        mBuilder.maxFlightSpeed(10f);
        mBuilder.setExitMissionOnRCSignalLostEnabled(false);
        mBuilder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        mBuilder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        mBuilder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        mBuilder.headingMode(WaypointMissionHeadingMode.AUTO);
        mBuilder.repeatTimes(1);
        mWaypointList = new ArrayList<>();
    }

    protected void finalizeNewMission() {
        mBuilder.waypointList(mWaypointList).waypointCount(mWaypointList.size());
        mModel.setWaypointMission(mBuilder.build());
    }

}
