/**
 * This software is copyright (c) 2013-2016 by
 *  - Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 * This is free software. You can redistribute it
 * and/or modify it under the terms described in
 * the GNU General Public License v3 of which you
 * should have received a copy. Otherwise you can download
 * it from
 *
 *   http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * @copyright Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.sru.fcs.validator;

import eu.clarin.sru.client.SRUVersion;

public enum FCSTestProfile {
    CLARIN_FCS_2_0("CLARIN FCS 2.0", SRUVersion.VERSION_2_0),
    CLARIN_FCS_1_0("CLARIN FCS 1.0", SRUVersion.VERSION_1_2),
    CLARIN_FCS_LEGACY("Legacy FCS", SRUVersion.VERSION_1_2),

    // Work-in-Progress
    LEX_FCS("LexFCS", SRUVersion.VERSION_2_0),

    // NOTE: we probably only want new clients to start with FCS 2.0
    AGGREGATOR_MIN_FCS("Minimum for FCS Aggregator", SRUVersion.VERSION_2_0);

    final SRUVersion version;
    final String displayString;

    private FCSTestProfile(String displString, SRUVersion version) {
        this.displayString = displString;
        this.version = version;
    }

    public SRUVersion getSRUVersion() {
        return version;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String toDisplayString() {
        return this.displayString;
    }

} // enum FCSTestProfile
