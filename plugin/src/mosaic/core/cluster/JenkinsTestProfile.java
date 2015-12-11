package mosaic.core.cluster;

/**
 * Hard coded implementation of the cherryphi-1 server
 */

class JenkinsTestProfile extends GeneralProfile {

    JenkinsTestProfile() {
        setAcc(hw.CPU);
        setQueue(9999, "normal");
        setBatchSystem(new LSFBatch(this));
    }

    @Override
    public String getProfileName() {
        return "Jenkins - cherryphi-1";
    }

    @Override
    public void setProfileName(String ProfileName_) {
    }

    @Override
    public String getAccessAddress() {
        return "cherryphi-1";
    }

    @Override
    public String getRunningDir() {
        return "/home/" + UserName + "/scratch/";
    }

    @Override
    public void setRunningDir(String RunningDir_) {
    }

    @Override
    public String getImageJCommand() {
        return "fiji";
    }

    @Override
    public void setImageJCommand(String ImageJCommand_) {
    }

    @Override
    public boolean hasCompressor(DataCompression.Algorithm a) {
        if (a == null) {
            return true;
        }

        if (a.name.equals("TAR")) {
            return true;
        }
        else if (a.name.equals("ZIP")) {
            return true;
        }

        return false;
    }
}
