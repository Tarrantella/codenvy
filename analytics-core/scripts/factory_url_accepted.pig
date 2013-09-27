/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

IMPORT 'macros.pig';

a1 = loadResources('$LOG', '$FROM_DATE', '$TO_DATE', '$USER', '$WS');
a2 = filterByEvent(a1, 'factory-url-accepted');
a3 = extractUrlParam(a2, 'REFERRER', 'referrer');
a4 = extractUrlParam(a3, 'FACTORY-URL', 'factoryUrl');
a5 = extractUrlParam(a4, 'ORG-ID', 'orgId');
a6 = extractUrlParam(a5, 'AFFILIATE-ID', 'affiliateId');
a = FOREACH a6 GENERATE ws, referrer, factoryUrl, orgId, affiliateId;

b = LOAD '$LOAD_DIR' USING PigStorage() AS (ws : chararray, referrer : chararray, factoryUrl : chararray,
                                            orgId : chararray, affiliateId : chararray);
c1 = UNION a, b;
c = DISTINCT c1;

STORE c INTO '$STORE_DIR' USING PigStorage();

r1 = FOREACH a GENERATE ws;
result = DISTINCT r1;
