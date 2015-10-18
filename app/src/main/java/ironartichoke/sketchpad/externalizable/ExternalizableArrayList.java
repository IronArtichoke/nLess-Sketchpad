package ironartichoke.sketchpad.externalizable;

import android.support.annotation.NonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An externalizable form of {@link ArrayList}.
 * @param <E> Any class to be contained in this array that extends {@link Externalizable}.
 * @see ArrayList
 */
public class ExternalizableArrayList<E extends Externalizable> extends ArrayList<E> implements Externalizable, Cloneable
{
	/**
	 * @see ArrayList#ArrayList()
	 */
	public ExternalizableArrayList()
	{
		super();
	}

	/**
	 * @see ArrayList#ArrayList(int)
	 */
	@SuppressWarnings("unused")
	public ExternalizableArrayList(int capacity)
	{
		super(capacity);
	}

	/**
	 * @see ArrayList#ArrayList(Collection)
	 */
	@SuppressWarnings("unused")
	public ExternalizableArrayList(Collection<? extends E> collection)
	{
		super(collection);
	}

	/**
	 * Returns a new <code>ExternalizableArrayList</code>, containing all the elements of the original.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Object clone()
	{
		return (ExternalizableArrayList<E>) super.clone();
	}

	@Override
	public boolean add(E object)
	{
		return super.add(object);
	}

	@Override
	public void add(int index, E object)
	{
		super.add(index, object);
	}

	/**
	 * <b>This method is unsupported.</b>
	 */
	@Override
	public boolean addAll(Collection<? extends E> collection)
	{
		throw new UnsupportedOperationException("addAll(Collection) is not supported.");
	}

	/**
	 * <b>This method is unsupported.</b>
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> collection)
	{
		throw new UnsupportedOperationException("addAll(int, Collection) is not supported.");
	}

	@Override
	public E remove(int index)
	{
		return super.remove(index);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object object)
	{
		return super.remove(object);
	}

	/**
	 * <b>This method is unsupported.</b>
	 */
	@Override
	protected void removeRange(int fromIndex, int toIndex)
	{
		throw new UnsupportedOperationException("removeRange(int, int) is not supported.");
	}

	/**
	 * <b>This method is unsupported.</b>
	 */
	@Override
	public boolean removeAll(@NonNull Collection<?> collection)
	{
		throw new UnsupportedOperationException("removeAll(Collection) is not supported.");
	}

	@Override
	public void clear()
	{
		super.clear();
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException
	{
		output.writeInt(size()); // Size of array
		for (E e : this) // Elements
		{
			output.writeObject(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException
	{
		int size = input.readInt(); // Size of array
		while (size-- > 0) // Elements
		{
			add((E) input.readObject());
		}
	}
}
