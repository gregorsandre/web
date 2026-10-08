import type { Availability, EnvPref, Goal, LookingFor } from '../api/users'

export const GOAL_LABELS: Record<Goal, string> = {
  fun: 'Fun',
  fitness: 'Fitness',
  competition: 'Competition',
  event_training: 'Training for an event',
}

export const LOOKING_FOR_LABELS: Record<LookingFor, string> = {
  regular_partner: 'A regular partner',
  casual_games: 'Casual games',
}

export const ENV_PREF_LABELS: Record<EnvPref, string> = {
  indoor: 'Indoor',
  outdoor: 'Outdoor',
  both: 'Indoor or outdoor',
}

export const AVAILABILITY_LABELS: Record<Availability, string> = {
  weekday_morning: 'Weekday mornings',
  weekday_evening: 'Weekday evenings',
  weekend: 'Weekends',
}

export const SKILL_LABELS = ['Beginner', 'Novice', 'Intermediate', 'Advanced', 'Expert']

export function skillLabel(level: number): string {
  return SKILL_LABELS[level - 1] ?? String(level)
}
