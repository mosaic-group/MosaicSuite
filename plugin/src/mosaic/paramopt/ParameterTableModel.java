package mosaic.paramopt;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import mosaic.plugin.macro.ParameterSettings;
import mosaic.plugin.macro.ParameterizedMacro;

public class ParameterTableModel extends AbstractTableModel {

	// TODO: Generate serialVersionUID when class is completed!
	private static final long serialVersionUID = 1L;

	private List<ParameterSettings> parameters;

	protected ParameterizedMacro pmacro;

	public ParameterTableModel() {
		super();
	}

	public void setMacro(ParameterizedMacro pmacro) {
		if (pmacro == null)
			throw new IllegalArgumentException("pmacro cannot be null!");
		
		this.pmacro = pmacro;
		parameters = pmacro.getParameterSettings();
	}
	
	public int getRowCount() {
		return parameters.size();
	}

	public int getColumnCount() {
		return 6;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (rowIndex < parameters.size()) {
			switch (columnIndex) {
			case 0:
			case 1:
				return false; // can't change method name and parameter name
			default:
				return true;
			}
		} else
			return false;
	}

	public Class<?> getColumnClass(int c) {
		switch (c) {
		case 0:
		case 1:
			return String.class;
		case 2:
			return Boolean.class;
		case 3:
		case 4:
		case 5:
			return Double.class;
		default:
			return null;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex < parameters.size()) {
			ParameterSettings param = parameters.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return param.getMethodName();
			case 1:
				return param.getName();
			case 2:
				return param.isEnabled();
			case 3:
				return param.getInitialValue();
			case 4:
				return param.getLowerBound();
			case 5:
				return param.getUpperBound();
			default:
				return null;
			}
		} else
			return null;
	}

	public String getColumnName(int col) {
		switch (col) {
		case 0:
			return "Method";
		case 1:
			return "Parameter";
		case 2:
			return "Optimize";
		case 3:
			return "Initial Value";
		case 4:
			return "Lower Bound";
		case 5:
			return "Upper Bound";
		default:
			return null;
		}
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (rowIndex >= 0 && rowIndex < parameters.size()) {
			ParameterSettings param = parameters.get(rowIndex);
			switch (columnIndex) {
			case 0:
			case 1:
				return;
			case 2:
				param.setEnabled(((Boolean) value).booleanValue());
				fireTableCellUpdated(rowIndex, columnIndex);
				return;
			case 3:
				param.setInitialValue(((Double) value).doubleValue());
				return;
			case 4:
				param.setLowerBound((Double) value);
				return;
			case 5:
				param.setUpperBound((Double) value);
				return;
			default:
				return;
			}
		} else
			return;
	}
}