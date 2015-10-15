package mosaic.core.cluster;


import java.util.Vector;

import mosaic.core.cluster.DataCompression.Algorithm;


/**
 * It implement the interface Cluster Profile and it store all the information of a cluster
 *
 * @author Pietro Incardona
 */

class GeneralProfile implements ClusterProfile {

    private BatchInterface bc;
    private String ProfileName;
    private String RunningDir;
    private String ImageJCommand;
    private String AccessAddress;
    private String pwd;
    private hw acc;
    protected String UserName;

    private class Tqueue {

        Tqueue(double minutes_, String name_) {
            minutes = minutes_;
            name = name_;
        }

        double minutes;
        String name;
    }

    private final Vector<Vector<Tqueue>> cq;

    GeneralProfile() {
        cq = new Vector<Vector<Tqueue>>();
    }

    @Override
    public void setUsername(String UserName_) {
        UserName = UserName_;
    }

    @Override
    public String getUsername() {
        return UserName;
    }

    @Override
    public void setPassword(String pwd_) {
        pwd = pwd_;
    }

    @Override
    public String getPassword() {
        return pwd;
    }

    @Override
    public String getProfileName() {
        return ProfileName;
    }

    @Override
    public void setProfileName(String ProfileName_) {
        ProfileName = ProfileName_;
    }

    @Override
    public String getRunningDir() {
        return RunningDir;
    }

    @Override
    public void setRunningDir(String RunningDir_) {
        RunningDir = RunningDir_;
    }

    @Override
    public String getImageJCommand() {
        return ImageJCommand;
    }

    @Override
    public void setImageJCommand(String ImageJCommand_) {
        ImageJCommand = ImageJCommand_;
    }

    @Override
    public String getQueue(double minutes) {
        for (int i = 0; i < cq.size(); i++) {
            if (cq.get(acc.ordinal()).get(i).minutes > minutes) {
                return cq.get(acc.ordinal()).get(i).name;
            }
        }

        return null;
    }

    @Override
    public void setAcc(hw acc_) {
        acc = acc_;
    }

    @Override
    public void setQueue(double minutes, String name) {
        if (cq.size() <= acc.ordinal()) {
            cq.add(new Vector<Tqueue>());
        }

        for (int i = 0; i < cq.get(acc.ordinal()).size(); i++) {
            if (cq.get(acc.ordinal()).get(i).minutes > minutes) {
                cq.get(acc.ordinal()).insertElementAt(new Tqueue(minutes, name), i);
                return;
            }
        }

        cq.get(acc.ordinal()).add(new Tqueue(minutes, name));
    }

    @Override
    public String getAccessAddress() {
        return AccessAddress;
    }

    @Override
    public void setAccessAddress(String AccessAddress_) {
        AccessAddress = AccessAddress_;
    }

    @Override
    public void setBatchSystem(BatchInterface bc_) {
        bc = bc_;
    }

    @Override
    public BatchInterface getBatchSystem() {
        return bc;
    }

    // TODO: WTF? alc is never assigned but searched in hasCompressor method (temporarily commented out)
//    private final Vector<Algorithm> alc;

    @Override
    public boolean hasCompressor(Algorithm a) {
//        for (int i = 0; i < alc.size(); i++) {
//            if (alc.get(i) == a) {
//                return true;
//            }
//        }

        return true;
    }

    @Override
    public QueueProfile[] getQueues(hw Acc_) {
        if (Acc_.ordinal() >= cq.size()) {
            return new QueueProfile[0];
        }

        final QueueProfile[] cpt = new QueueProfile[cq.get(Acc_.ordinal()).size()];

        for (int i = 0; i < cq.get(Acc_.ordinal()).size(); i++) {
            cpt[i] = new QueueProfile();
            cpt[i].queue = cq.get(Acc_.ordinal()).get(i).name;
            cpt[i].hardware = Acc_.toString();
            cpt[i].limit = cq.get(Acc_.ordinal()).get(i).minutes;
        }

        return cpt;
    }
}
