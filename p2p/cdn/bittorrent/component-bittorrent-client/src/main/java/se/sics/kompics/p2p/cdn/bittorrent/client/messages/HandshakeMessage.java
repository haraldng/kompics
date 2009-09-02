/**
 * This file is part of the Kompics P2P Framework.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
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
package se.sics.kompics.p2p.cdn.bittorrent.client.messages;

import java.math.BigInteger;

import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;
import se.sics.kompics.p2p.cdn.bittorrent.message.BitTorrentMessage;

/**
 * The <code>HandshakeMessage</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class HandshakeMessage extends BitTorrentMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1176854506654134822L;

	private final BigInteger torrentId;

	private final boolean response;

	public HandshakeMessage(BitTorrentAddress source,
			BitTorrentAddress destination, BigInteger torrentId,
			boolean response) {
		super(source, destination);
		this.torrentId = torrentId;
		this.response = response;
	}

	public BigInteger getTorrentId() {
		return torrentId;
	}

	public boolean isResponse() {
		return response;
	}

	@Override
	public int getSize() {
		return 45;
	}
}
