/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.network.data.test;

import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.data.DataHeader;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class Data extends DataMessage {

    public final int pos;
    public final int total;
    public final byte[] data;

    public Data(Address src, Address dst, Transport proto, int pos, int total, byte[] data) {
        super(src, dst, proto);
        this.pos = pos;
        this.total = total;
        this.data = data;
    }

    Data(DataHeader<Address> header, int pos, int total, byte[] data) {
        super(header);
        this.pos = pos;
        this.total = total;
        this.data = data;
    }
}
