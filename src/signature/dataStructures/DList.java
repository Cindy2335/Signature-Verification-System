package signature.dataStructures;

import java.util.NoSuchElementException;
import java.util.Iterator;

public class DList<T> implements Iterable<T>
{
     public static class Node<T>
    {
        T value;
        public Node<T> next;
        public Node<T> prev;

        public Node(T value)
        {
            this.value = value;
            this.next = null;
            this.prev = null;
        }
    }

    private Node<T> first;
    private Node<T> last;
    private int size;

    public DList()
    {
    first = null;
    last = null;
    size = 0;


    }


    public DList(int initialCapacity)
    {
        this();
    }


    public void add(T value )
    {
        Node<T> node = new Node<T>(value);

        if(isEmpty())
        {
            first = node;
            last = node;

        }
        else
        {
            last.next = node;
            node.prev = last;
            last = node;
        }

        size++;
    }


    public void addFirst(T value)
    {
        Node<T> node = new Node<T>(value);

        if(isEmpty())
        {
            first = node;
            last = node;

        }
        else
        {
            first.prev = node;
            node.next = first;
            first = node;
        }

        size++;
    }

    public T get(int index)
    {
        return NodeAt(index).value;
    }

    public void set(int index, T value)
    {
        NodeAt(index).value = value;
    }

    public T removeLast()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("List is empty");

        }
        T value = last.value;
        last = last.prev;
        size--;

        if (isEmpty())
        {
            first = null;
        }
        else
        {
            last.next = null;
        }
        return value;
    }

    public int size()
    {
        return size;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    private Node<T> NodeAt(int index)
    {
        if (index < 0 || index >= size)
        {
            throw new IndexOutOfBoundsException("Index: " + index + ", outside Size: " + size);
        }

        Node<T> current;
        if(index < size /2)
        {
            current = first;
            for (int i = 0; i < index; i++)
            {
                current = current.next;
            }
        }
        else
        {
            current = last;
            for (int i = size - 1; i > index; i--)
            {
                current = current.prev;
            }
        }
        return current;

    }

    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<T>()
        {
            private Node<T> current = first;

            @Override
            public boolean hasNext()
            {
                return current != null;
            }

            @Override
            public T next()
            {
                if (!hasNext())
                {
                    throw new NoSuchElementException();
                }
                T value = current.value;
                current = current.next;
                return value;
            }
        };


    }







   

} 
