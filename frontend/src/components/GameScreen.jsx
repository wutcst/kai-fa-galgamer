import { useState } from 'react'
import BattlePanel from './BattlePanel'
import ChoicePanel from './ChoicePanel'
import DialogueBox from './DialogueBox'
import DialogueChoicePanel from './DialogueChoicePanel'
import DirectionButtons from './DirectionButtons'
import DualSpriteStage from './DualSpriteStage'
import ExplorationMap from './ExplorationMap'
import InventoryModal from './InventoryModal'
import MiniGameOutcomePanel from './MiniGameOutcomePanel'
import MiniGamePanel from './MiniGamePanel'
import PuzzleModal from './PuzzleModal'

function GameScreen({ snapshot, assets, loading, onAction, onOpenSave }) {
  const [inventoryOpen, setInventoryOpen] = useState(false)
  const sceneUrl = assets[snapshot.roomAssetKey] ?? assets.fallback
  const playerAvatar = assets['ui.player_avatar']
  const backpackIcon = assets['ui.backpack_icon']
  const newLogDot = assets['ui.new_log_red_dot']
  const puzzle = normalizePuzzle(snapshot)
  const actions = snapshot.availableActions ?? []
  const dialogue = snapshot.dialogue?.active ? snapshot.dialogue : null
  const utilityActions = actions.filter(
    (action) => actionType(action) !== 'MOVE'
      && !(['MINI_GAME_INPUT', 'ACK_MINI_GAME_RESULT', 'BATTLE_ACTION', 'CHOOSE_ENDING'].includes(actionType(action)))
      && !(puzzle && ['CRAFT', 'ANSWER'].includes(actionType(action))),
  )
  const inBattle = Boolean(snapshot.battle)
  const inChoice = Boolean(snapshot.ending || snapshot.choices?.length)
  const inAdvChoice = dialogue?.nodeType === 'CHOICE'

  const advanceDialogue = () => {
    if (!dialogue || inAdvChoice) {
      return
    }
    onAction({ actionType: 'ADV_NEXT', target: dialogue.currentNodeId, value: null })
  }

  const chooseDialogue = (choiceId) => {
    onAction({ actionType: 'ADV_CHOOSE', target: choiceId, value: null })
  }

  return (
    <main className="game-screen">
      <div className="scene-image" style={{ backgroundImage: `url("${sceneUrl}")` }} />
      <div className="scene-vignette" />
      <ExplorationMap map={snapshot.map} assets={assets} />

      <header className="top-hud">
        <div className="brand-mark">
          <strong>World of Zuul</strong>
          <span>Uncertain Fate</span>
        </div>
        <div className="hp-panel">
          {playerAvatar ? <img src={playerAvatar} alt="" /> : null}
          <span>HP</span>
          <strong>{snapshot.playerHp}</strong>
        </div>
        <div className="phase-panel">{snapshot.gamePhase}</div>
        <button className="inventory-hud-button" type="button" onClick={() => setInventoryOpen(true)}>
          {backpackIcon ? <img src={backpackIcon} alt="" /> : null}
          <span>背包</span>
          <strong>{snapshot.inventoryItems?.length ?? 0}</strong>
        </button>
      </header>

      {dialogue ? (
        <section className="adv-layout">
          <DualSpriteStage dialogue={dialogue} sceneUrl={sceneUrl} assets={assets} onStageClick={advanceDialogue}>
            {inAdvChoice ? (
              <DialogueChoicePanel choices={dialogue.choices ?? []} loading={loading} onChoose={chooseDialogue} />
            ) : (
              <DialogueBox dialogue={dialogue} loading={loading} onAdvance={advanceDialogue} />
            )}
          </DualSpriteStage>
        </section>
      ) : (
        <section className="narrative-layout">
          <article className="story-panel">
            <p className="room-id">{snapshot.currentRoomId}</p>
            <h1>{snapshot.roomTitle}</h1>
            <p>{snapshot.roomDescription}</p>
            {snapshot.systemMessage ? <strong className="system-message">{snapshot.systemMessage}</strong> : null}
            {snapshot.errorMessage ? <strong className="error-message">{snapshot.errorMessage}</strong> : null}
          </article>

          {snapshot.battle ? (
            <BattlePanel
              battle={snapshot.battle}
              assets={assets}
              loading={loading}
              playerHp={snapshot.playerHp}
              onAction={onAction}
            />
          ) : null}
          {!snapshot.battle && inChoice ? (
            <ChoicePanel snapshot={snapshot} assets={assets} loading={loading} onAction={onAction} />
          ) : null}
          {!inBattle && !inChoice && snapshot.miniGameOutcome ? (
            <MiniGameOutcomePanel outcome={snapshot.miniGameOutcome} assets={assets} loading={loading} onAction={onAction} />
          ) : null}
          {!inBattle && !inChoice && !snapshot.miniGameOutcome && snapshot.miniGame ? (
            <MiniGamePanel miniGame={snapshot.miniGame} assets={assets} loading={loading} onAction={onAction} />
          ) : null}
          {!inBattle && !inChoice && !snapshot.miniGameOutcome && !snapshot.miniGame && puzzle ? (
            <PuzzleModal puzzle={puzzle} loading={loading} assets={assets} onAction={onAction} />
          ) : null}

          <aside className="control-panel">
            <DirectionButtons actions={actions} assets={assets} onAction={onAction} disabled={loading} />

            <div className="action-row">
              {utilityActions.map((action) => (
                <button
                  key={`${action.actionType}-${action.target}`}
                  className="action-button"
                  type="button"
                  disabled={loading}
                  onClick={() =>
                    ['SAVE', 'LOAD'].includes(actionType(action)) && onOpenSave
                      ? onOpenSave()
                      : onAction({
                          actionType: action.actionType,
                          target: action.target,
                          value: null,
                        })
                  }
                >
                  {action.label}
                </button>
              ))}
            </div>
          </aside>
        </section>
      )}

      <aside className="log-panel">
        <h2>
          <span>探索日志</span>
          {newLogDot && snapshot.logs?.length ? <img src={newLogDot} alt="" /> : null}
        </h2>
        <ul>
          {snapshot.logs?.slice(-5).map((log, index) => (
            <li key={`${index}-${log}`}>{log}</li>
          ))}
        </ul>
      </aside>

      {inventoryOpen ? (
        <InventoryModal
          inventoryItems={snapshot.inventoryItems}
          assets={assets}
          loading={loading}
          onAction={onAction}
          onClose={() => setInventoryOpen(false)}
        />
      ) : null}
    </main>
  )
}

function normalizePuzzle(snapshot) {
  if (snapshot.puzzle) {
    return {
      ...snapshot.puzzle,
      options: snapshot.puzzle.options ?? [],
      freeText: Boolean(snapshot.puzzle.freeText ?? snapshot.puzzle.free_text ?? snapshot.puzzle.requiresInput),
      submitAction: snapshot.puzzle.submitAction ?? snapshot.puzzle.submit_action ?? 'ANSWER',
    }
  }

  const actions = snapshot.availableActions ?? []
  const answerAction = actions.find((action) => actionType(action) === 'ANSWER')
  if (answerAction) {
    return {
      id: answerAction.target,
      prompt: answerAction.label || '请输入谜题答案。',
      kind: 'GENERIC',
      options: [],
      freeText: true,
      submitAction: 'ANSWER',
    }
  }

  const craftAction = actions.find((action) => actionType(action) === 'CRAFT')
  if (craftAction) {
    return {
      id: craftAction.target,
      prompt: craftAction.label || '选择要合成的道具。',
      kind: 'ITEM_COMBINATION',
      options: [craftAction.target].filter(Boolean),
      freeText: false,
      submitAction: 'CRAFT',
    }
  }

  return null
}

function actionType(action) {
  return String(action?.actionType ?? '').toUpperCase()
}

export default GameScreen
