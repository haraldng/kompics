package se.sics.kompics.core.scheduler;

public class FreelistSpinlockQueue<E> {

	public static int SIZE = 1000;

	private Spinlock lock = new Spinlock();

	@SuppressWarnings("unchecked")
	static final ThreadLocal<FreeList> freeList = new ThreadLocal<FreeList>() {
		@Override
		protected FreeList initialValue() {
			return new FreeList(null, 0);
		};
	};

	static class FreeList<E> {
		Node<E> head;
		int size;
		int foundEmpty;
		int foundFull;
//		int allocated;
//		int freed;

		public FreeList(Node<E> next, int size) {
			this.head = next;
			this.size = size;
		}
	}

	@SuppressWarnings("unchecked")
	private Node<E> allocate(E value, Node<E> next) {
		FreeList<E> free = freeList.get();
		Node<E> node = free.head;
//		free.allocated++;
		if (node == null) { // nothing to recycle
			free.foundEmpty++;
			return new Node<E>(value, next);
		}
		// recycle existing node
		free.head = node.next;
		free.size--;
		// initialize
		node.item = value;
		node.next = next;
		return node;
	}

	@SuppressWarnings("unchecked")
	private void free(Node<E> node) {
		FreeList<E> free = freeList.get();
//		free.freed++;
		if (free.size < SIZE) {
			node.next = free.head;
			node.item = null;
			free.head = node;
			free.size++;
			return;
		}
		free.foundFull++;
	}

	// @SuppressWarnings("unchecked")
	// private void free(Node<E> node) {
	// Head<E> free = freeList.get();
	// node.next = free.next;
	// node.item = null;
	// free.next = node;
	// free.size++;
	//
	// if (free.size >= SIZE) {
	// free.next = null;
	// free.size = 0;
	// }
	// }

	static class Node<E> {
		E item;
		Node<E> next;

		public Node(E item, Node<E> next) {
			this.item = item;
			this.next = next;
		}
	}

	private Node<E> head;
	private Node<E> tail;

	public FreelistSpinlockQueue() {
		head = new Node<E>(null, null);
		tail = head;
	}

	public void offer(E e) {
		if (e == null)
			throw new NullPointerException();
		Node<E> n = allocate(e, null);
		lock.lock();
		try {
			tail.next = n;
			tail = n;
		} finally {
			lock.unlock();
		}
	}

	public E poll() {
		E e = null;
		Node<E> removed = null;
		lock.lock();
		try {
			if (head == tail)
				return null;
			removed = head;
			head = head.next;
			e = head.item;
		} finally {
			lock.unlock();
		}
		free(removed);
		return e;
	}

	@SuppressWarnings("unchecked")
	public static int[] getStats() {
		FreeList free = freeList.get();
		int ret[] = new int[4];
		ret[0] = free.foundEmpty;
		ret[1] = free.foundFull;
//		ret[2] = free.allocated;
//		ret[3] = free.freed;
		return ret;
	}

	@SuppressWarnings("unchecked")
	public static void resetStats() {
		FreeList free = freeList.get();
		free.foundEmpty = 0;
		free.foundFull = 0;
//		free.allocated = 0;
//		free.freed = 0;
	}
}