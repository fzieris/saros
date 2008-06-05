/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.activities;

/**
 * A generic interface for activities that happen in sessions. All activities
 * should be implemented by the value patter, i.e. created activities are
 * immutable-
 * 
 * @author rdjemili
 */
public interface IActivity {
	
	/**
	 * set source jabber_id of producer of this IActivity.
	 * @param source jabber_id string
	 */
	public void setSource(String source);
	
	/**
	 * get jabber_id of remote producer
	 * @return jabber_id
	 */
	public String getSource();
}
