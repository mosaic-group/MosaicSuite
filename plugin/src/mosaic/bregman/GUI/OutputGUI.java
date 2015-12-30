package mosaic.bregman.GUI;


import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;


public class OutputGUI extends JDialog {

    private static final long serialVersionUID = 1L;
    protected final JDialog myself;

    private class OutputTable extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] columnNames = { "Enable", "Column", "Factor" };

        Object[][] data;

        protected OutputTable() {}

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        @Override
        public boolean isCellEditable(int row, int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            if (col == 0 || col == 2) {
                return true;
            }
            else {
                return false;
            }
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        @Override
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }

        void setOutput(GUIOutputChoose out) {
            data = new Object[out.outputChoose.fieldMapping.length][3];

            for (int i = 0; i < out.outputChoose.fieldMapping.length; i++) {
                data[i][0] = new Boolean(true);
                data[i][1] = out.outputChoose.fieldMapping[i];
                data[i][2] = new Double(1.0);
            }
        }
    }

    private JPanel contentPane;
    private JTable table;
    protected GUIOutputChoose oc;
    protected GUIOutputChoose out[];
    protected OutputTable outTM;

    public OutputGUI() {
        oc = null;
        myself = this;
    }

    /**
     * Visualize a window to select all the possible output format and related option
     *
     * @param out_ all Output format
     * @param selected oc_s
     * @return Choose output + information on the option choose
     */

    public GUIOutputChoose visualizeOutput(GUIOutputChoose out_[], int oc_s) {
        if (out_ == null) {
            return null;
        }
        if (oc != null) {
            return oc;
        }

        out = out_;
        final String[] pn = new String[out.length];

        for (int i = 0; i < out.length; i++) {
            pn[i] = out[i].name;
        }

        setBounds(0, 0, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 0, 0);
        JLabel lblNewLabel = new JLabel("<html><h2><font color=\"red\">CSV Format</font></h2></html>");
        lblNewLabel.setMinimumSize(new Dimension(300, 30));
        contentPane.add(lblNewLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        lblNewLabel = new JLabel("Choose output format ");
        contentPane.add(lblNewLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        final JComboBox<String> comboBox = new JComboBox<String>(pn);
        comboBox.setSelectedIndex(oc_s);
        oc = out[oc_s];
        contentPane.add(comboBox, c);
        outTM = new OutputTable();
        comboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getSource() == comboBox) {
                    final int idx = comboBox.getSelectedIndex();
                    oc = out[idx];
                    outTM.setOutput(out[idx]);
                    outTM.fireTableDataChanged();
                }
            }
        });

        outTM.setOutput(out[oc_s]);
        table = new JTable(outTM);

        final JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        scrollPane.setMinimumSize(new Dimension(450, 150));

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 0, 10);
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(scrollPane, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.insets = new Insets(10, 0, 0, 10);
        c.fill = GridBagConstraints.NONE;

        final JButton bOK = new JButton("OK");
        bOK.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                myself.dispose();

            }

        });

        contentPane.add(bOK, c);

        c.gridx = 1;
        c.gridy = 3;
        final JButton bCancel = new JButton("Cancel");
        contentPane.add(bCancel, c);

        setModal(true);
        setVisible(true);

        oc.factor = new double[oc.outputChoose.fieldMapping.length];
        for (int i = 0; i < oc.outputChoose.fieldMapping.length; i++) {
            oc.factor[i] = (Double) outTM.data[i][2];
        }

        return oc;
    }
}
