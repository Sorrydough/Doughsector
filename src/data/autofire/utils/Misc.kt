package data.autofire.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.FastTrig
import org.lazywizard.lazylib.MathUtils.getShortestRotation
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.getFacing
import org.lazywizard.lazylib.ext.minus
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.util.vector.Vector2f
import kotlin.math.PI
import kotlin.math.abs

/**
 *  Find the distance between point (0,0)
 *  and point p traveling with velocity v.
 *
 *  Returns null if v points away from (0,0).
 */
fun distanceToOrigin(p: Vector2f, v: Vector2f): Float? {
    val t = -(p.x * v.x + p.y * v.y) / (v.x * v.x + v.y * v.y)
    return if (t >= 0) (p + v * t).length()
    else null
}

// TODO remove and use ballistics implementation
fun willHitShield(weapon: WeaponAPI, target: ShipAPI?) = when {
    target == null -> false
    target.shield == null -> false
    target.shield.isOff -> false
    else -> willHitActiveShieldArc(weapon, target.shield)
}

// TODO remove and use ballistics implementation
fun willHitActiveShieldArc(weapon: WeaponAPI, shield: ShieldAPI): Boolean {
    val tgtFacing = (weapon.location - shield.location).getFacing()
    val attackAngle = getShortestRotation(tgtFacing, shield.facing)
    return abs(attackAngle) < (shield.activeArc / 2)
}

fun shieldUptime(shield: ShieldAPI?): Float {
    if (shield == null) return 0f
    val r = shield.activeArc / shield.arc
    return if (r >= 1f) Float.MAX_VALUE
    else r * shield.unfoldTime
}

internal infix operator fun Vector2f.times(d: Float): Vector2f = Vector2f(x * d, y * d)
internal infix operator fun Vector2f.div(d: Float): Vector2f = Vector2f(x / d, y / d)

fun rotateAroundPivot(toRotate: Vector2f, pivot: Vector2f, angle: Float): Vector2f = VectorUtils.rotateAroundPivot(toRotate, pivot, angle, Vector2f())

fun unitVector(angle: Float): Vector2f = VectorUtils.rotate(Vector2f(1f, 0f), angle)

fun atan(radians: Float): Float = Math.toDegrees(FastTrig.atan(radians.toDouble())).toFloat()

data class Arc(val arc: Float, val facing: Float)

fun vectorInArc(v: Vector2f, a: Arc): Boolean = abs(getShortestRotation(VectorUtils.getFacing(v), a.facing)) <= a.arc / 2f

fun arcsOverlap(a: Arc, b: Arc): Boolean = abs(getShortestRotation(a.facing, b.facing)) <= (a.arc + b.arc) / 2f

class Log

fun log(message: Any) = Global.getLogger(Log().javaClass).info(message)

class Rotation(angle: Float) {
    private val radians = angle / 180.0f * PI.toFloat()
    private val sin = kotlin.math.sin(radians)
    private val cos = kotlin.math.cos(radians)

    fun rotate(v: Vector2f) = Vector2f(v.x * cos - v.y * sin, v.x * sin + v.y * cos)
}

fun defaultAIInterval() = IntervalUtil(0.25f, 0.50f)