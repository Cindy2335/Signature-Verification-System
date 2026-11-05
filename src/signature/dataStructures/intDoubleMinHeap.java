package signature.dataStructures;


public class intDoubleMinHeap {
 public static class HeapEntry
 {
     public final int node;
     public final double priority;

     public HeapEntry(int node, double priority)
     {
         this.node = node;
         this.priority = priority;
     }
 }

public void push(int node, double priority)
    {
        ensureCapacity(size + 1);
        nodes[size] = node;
        priorities[size] = priority;
        shiftup(size);
        size++;
    }


private int nodes[];
    private double[] priorities;
    private int size;


    public intDoubleMinHeap()
    {
        this.nodes = new int[16];
        this.priorities = new double[16];
        this.size = 0;
    }

    public HeapEntry pop()
    {
        if(size == 0)
        {
            throw new IllegalStateException("Heap is empty");
        }


         HeapEntry entry = new HeapEntry(nodes[0], priorities[0]);
         size--;
         nodes[0] = nodes[size];
         priorities[0] = priorities[size]; 
         shiftdown(0);
         return entry;
        


    }

    public void shiftdown(int index)
    {
        int parent = index;

        while(true)
        {
            int left = (parent * 2) + 1;
            int right = left + 1;
            int smallest = parent;

            if(left < size && priorities[left] < priorities[smallest])
        {
            smallest = left;
        }
        if(right < size && priorities[right] < priorities[smallest])
        {
            smallest = right;
        }
        if(smallest == parent)
        {
            return;
        }

        swap(parent, smallest);
        parent = smallest;

        }
    }

    public void shiftup(int index)
    {
        int child = index;

        while(child > 0)
        {
            int parent = (child - 1)/2;
            if(priorities[parent] <= priorities[child])
            {
                return;
            }

            swap(parent, child);
            child = parent;
        }
    }
    

    public void swap(int i, int j)
    {
        int tempNode = nodes[i];
        double tempPriority = priorities[i];

        nodes[i] = nodes[j];
        priorities[i] = priorities[j];

        nodes[j] = tempNode;
        priorities[j] = tempPriority;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    public void ensureCapacity(int required)
    {
        if(size <= priorities.length)
        {

            if(required  <= nodes.length)
            {
                return;

            }

            int newCapacity = nodes.length * 2;
            while(newCapacity < required)
            {
                newCapacity *= 2;
            }

            int[] newNodes = new int[newCapacity];
            double[] newPriorities = new double[newCapacity];
            for(int i = 0; i < size; i++)
            {
                newNodes[i] = nodes[i];
                newPriorities[i] = priorities[i];
            }
            nodes = newNodes;
            priorities = newPriorities;
        }
    }

    
}
