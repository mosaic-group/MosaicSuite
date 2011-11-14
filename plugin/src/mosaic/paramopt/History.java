package mosaic.paramopt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class History {
	
	protected List<Record> records;
	protected int currentGeneration = 0;
	
	public History() {
		// Initialize the lists.
		records = new Vector<Record>();
	}
	
	public void addGeneration(double[][] values, double[] fitness) {
		// Requires that values and fitness have the same length
		if (values == null || fitness == null 
				|| values.length != fitness.length)
			return;
		
		// Add each individual as a record to the history.
		for (int i = 0; i < values.length; i++)
			records.add(new Record(currentGeneration, values[i], fitness[i]));
		
		// Increase generation counter.
		currentGeneration++;
	}
	
	/**
	 * Writes the history of the evolution strategy into a file which can be
	 * imported into MATLAB. The file has the following form
	 * 
	 * values=[1,1,1;2,2,2;3,3,3;4,4,4];
	 * generation=[1;1;2;2];
	 * fitness=[1.0;2.0;3.0;4.0];
	 * 
	 * Where in this example there are 2 3-dimensional individuals in each
	 * generation and just 2 generations are recorded.
	 * For the sake of simplicity in this example the i-th individual has values
	 * all i and a fitness of i. 
	 * 
	 * @param file
	 *            the file to which the history is to be written
	 * @throws IOException 
	 */
	public void writeToFile(File file) throws IOException {
		if (file == null)
			return;
		
		// Create the buffered writer.
		FileWriter writer = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(writer);
		
		// The header with information about this file.
		out.write("% This file contains the history of the parameter values " +
				"from the parameter optimization plugin.\n" +
				"% For each individual there is a row in the values matrix, " +
				"the fitness vector and the generation vector.\n");
		
		String values = "";
		String generation = "";
		String fitness = "";
		// Combine configuration of all parameters into the corresponding
		// strings.
		for (Record r : records) {
			values += array2String(r.getValue()) + ";";
			generation += r.getGeneration() + ";";
			fitness += r.getFitness() + ";";
		}
		
		// 
		out.write("values=[" + values + "];\n");
		out.write("generation=[" + generation + "];\n");
		out.write("fitness=[" + fitness + "];\n");
		
		// Close the writer.
		out.close();
	}
	
	/**
	 * Creates a string in the form of a MATLAB column vector out of the
	 * specified array of doubles.
	 * 
	 * @param values
	 *            the array with the values which are being encoded into a
	 *            MATLAB column vector
	 * @return the string in the MATLAB column vector format
	 */
	private String array2String(double[] values) {
		if (values == null || values.length == 0)
			return "";
		String result = "[" + values[0];
		for (int i = 1; i < values.length; i++)
			result += "," + values[i];
		return result + "]";
	}
	
	/**
	 * Small helper class which contains the records of this history structure.
	 */
	protected class Record {
		private final int generation;
		private final double[] value;
		private final double fitness;
		public Record(int generation, double[] value, double fitness) {
			this.generation = generation;
			this.value = value;
			this.fitness = fitness;
		}

		public int getGeneration() {
			return generation;
		}
		
		public double[] getValue() {
			return value;
		}
		
		public double getFitness() {
			return fitness;
		}
	}

}
