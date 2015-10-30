package mosaic.core.cluster;

/**
 * Hard coded implementation of the Mad Max profile
 *
 * @author Pietro Incardona
 */

class MadMaxProfile extends GeneralProfile {

    MadMaxProfile() {
        setAcc(hw.CPU);
        setQueue(60, "short");
        setQueue(480, "medium");
        setQueue(Double.MAX_VALUE, "long");

        setAcc(hw.GPU);
        setQueue(720, "gpu");
        setBatchSystem(new LSFBatch(this));
        setAcc(hw.CPU);
    }

    @Override
    public String getProfileName() {
        return "Mad Max";
    }

    @Override
    public void setProfileName(String ProfileName_) {
    }

    @Override
    public String getAccessAddress() {
        return "falcon";
    }

    @Override
    public String getRunningDir() {
        return "/scratch/users/" + UserName + "/";
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
