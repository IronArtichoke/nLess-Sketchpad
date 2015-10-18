package ironartichoke.sketchpad.externalizable;

import com.carrotsearch.hppc.ArraySizingStrategy;
import com.carrotsearch.hppc.FloatArrayList;
import com.carrotsearch.hppc.FloatContainer;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * An externalizable form of {@link FloatArrayList}.
 * @see FloatArrayList
 */
public class ExternalizableFloatArrayList extends FloatArrayList implements Externalizable, Cloneable
{
	/**
	 * Empty constructor for serialization.
	 */
	public ExternalizableFloatArrayList() {}

	/**
	 * @see FloatArrayList#FloatArrayList(int)
	 */
	@SuppressWarnings("unused")
	public ExternalizableFloatArrayList(int initialCapacity)
	{
		super(initialCapacity);
	}

	/**
	 * @see FloatArrayList#FloatArrayList(int, ArraySizingStrategy)
	 */
	@SuppressWarnings("unused")
	public ExternalizableFloatArrayList(int initialCapacity, ArraySizingStrategy resizer)
	{
		super(initialCapacity, resizer);
	}

	/**
	 * @see FloatArrayList#FloatArrayList(FloatContainer)
	 */
	@SuppressWarnings("unused")
	public ExternalizableFloatArrayList(FloatContainer container)
	{
		super(container);
	}

	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException
	{
		int size = input.readInt(); // Size of array
		while (size-- > 0) // Elements
		{
			add(input.readFloat());
		}
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException
	{
		int size = size();
		output.writeInt(size); // Size of array
		for (int i = 0; i < size; i++) // Elements
		{
			output.writeFloat(get(i));
		}
	}

	@Override
	public ExternalizableFloatArrayList clone()
	{
		return (ExternalizableFloatArrayList) super.clone();
	}
}
