/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import com.flowcrypt.email.DoesNotNeedMailserver
import org.junit.runner.Description

/**
 * @author Denis Bondarenko
 *         Date: 2/17/21
 *         Time: 5:06 PM
 *         E-mail: DenBond7@gmail.com
 */
class DependsOnMailServerFilter : ReadyForCIFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClass = DoesNotNeedMailserver::class.java
    val hasClassAnnotation = description?.testClass?.isAnnotationPresent(annotationClass) == true
    if (hasClassAnnotation) return false
    return super.evaluateTest(description) && description?.getAnnotation(annotationClass) == null
  }

  override fun describe() = "Filter tests that depend on an email server"
}