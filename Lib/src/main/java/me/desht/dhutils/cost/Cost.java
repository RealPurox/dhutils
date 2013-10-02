package me.desht.dhutils.cost;

import me.desht.dhutils.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public abstract class Cost {
	private final double quantity;

	protected Cost(double quantity) {
		this.quantity = quantity;
	}

	public double getQuantity() {
		return quantity;
	}

    /**
     * Parse a cost specification and return a new cost object.
     *
     * @param costSpec the cost specification
     * @return the new Cost object
     * @throws IllegalArgumentException if the specification is not valid
     */
	public static Cost parse(String costSpec) {
		String[] itemAndQuantity = costSpec.split(",");
		double q = itemAndQuantity.length < 2 ? 1.0 : Double.parseDouble(itemAndQuantity[1]);

		String[] typeAndData = itemAndQuantity[0].split(":");
		Validate.isTrue(typeAndData.length >= 1 && typeAndData.length <= 2, "cost: item format must be <id[:data]>");
		String costType = typeAndData[0].toUpperCase();
        short data = typeAndData.length == 2 ? Short.parseShort(typeAndData[1]) : 0;

		if (costType.equals("F")) {
			return new FoodCost(q);
		} else if (costType.equals("H")) {
			return new HealthCost(q);
		} else if (costType.equals("E")) {
			return new EconomyCost(q);
		} else if (costType.equals("X")) {
			return new ExperienceCost(q);
		} else if (costType.length() > 1) {
			// could be a material name or potion name
			Material mat = Material.matchMaterial(costType);
			if (mat != null) {
                if (itemAndQuantity.length > 2 && itemAndQuantity[2].toUpperCase().startsWith("D")) {
                    // item durability
                    return new DurabilityCost(mat, q);
                } else {
                    // it's a material name
                    return new ItemCost(mat, data, q);
                }
			} else {
				PotionEffectType pt = PotionEffectType.getByName(costType);
				if (pt != null) {
                    // it's a potion name
                    return new PotionCost(pt, data, q);
				} else {
                    throw new IllegalArgumentException("Cost: unknown material or potion type: '" + costType + "'");
                }
			}
        } else if (StringUtils.isNumeric(costType)) {
            // deprecated case: numeric item ID
            int id = Integer.parseInt(costType);
            Material mat = Material.getMaterial(id);
            Validate.notNull(mat, "Cost: invalid material ID: '" + costType + "'");
            LogUtils.warning("Cost: Numeric item ID in cost spec '" + costSpec + "' is deprecated; " +
                    "please use material name '" + mat + "' instead of item ID " + id + ".");
            return new ItemCost(mat, data, q);
        } else {
			throw new IllegalArgumentException("Cost: unknown cost type '" + costType + "'");
		}
	}

    /**
     * Charge a list of costs to the given player.
     *
     * @param player the player to apply the costs to
     * @param costs	a list of costs
     */
    public static void apply(Player player, List<Cost> costs) {
        for (Cost c : costs) {
            c.apply(player);
        }
    }
    /**
     * Check if the costs are applicable to the given player.
     *
     * @param player the player to check
     * @param costs a list of costs
     * @return true if the costs are applicable, false otherwise
     */
    public static boolean isApplicable(Player player, List<Cost> costs) {
        for (Cost c : costs) {
            if (!c.isApplicable(player))
                return false;
        }
        return true;
    }

    /**
     * Check if the player can afford to pay the costs.
     *
     * @param player the player to check
     * @param costs a list of costs
     * @return true if the costs are affordable, false otherwise
     */
    public static boolean isAffordable(Player player, List<Cost> costs) {
        for (Cost c : costs) {
            if (!c.isAffordable(player))
                return false;
        }
        return true;
    }

	public abstract String getDescription();
	public abstract boolean isAffordable(Player player);
	public abstract void apply(Player player);

	public boolean isApplicable(Player player) {
		return true;
	}

	protected double getAdjustedQuantity(int original, double adjust, double min, double max) {
		double newQuantity = original - adjust;
		if (newQuantity < min) {
			newQuantity = min;
		} else if (newQuantity > max) {
			newQuantity = max;
		}
		return newQuantity;
	}
}
