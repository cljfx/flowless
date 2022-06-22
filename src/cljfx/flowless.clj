(ns cljfx.flowless
  (:require [cljfx.api :as fx]
            [cljfx.composite :as composite]
            [cljfx.fx.region :as fx.region]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.coerce :as coerce]
            [cljfx.mutator :as mutator]
            [cljfx.component :as component])
  (:import [org.fxmisc.flowless VirtualFlow Cell VirtualizedScrollPane VirtualFlow$Gravity]
           [javafx.collections FXCollections]
           [java.util.function Function]
           [java.util Collection]
           [javafx.scene.control ScrollPane$ScrollBarPolicy]))

(def ^:private cell-factory-lifecycle
  (reify lifecycle/Lifecycle
    (create [this describe opts]
      (let [state-vol (volatile! {:describe describe
                                  :opts opts
                                  :slots {}})]
        (with-meta {:state state-vol
                    :instance (reify Function
                                (apply [_ item]
                                  (let [{:keys [describe opts]} @state-vol
                                        desc (describe item)
                                        component (lifecycle/create lifecycle/dynamic desc opts)
                                        cell (reify Cell
                                               (getNode [_]
                                                 (fx/instance component))
                                               (dispose [this]
                                                 (let [{:keys [slots opts]} @state-vol]
                                                   (lifecycle/delete lifecycle/dynamic (:component (get slots this)) opts)
                                                   (vswap! state-vol update :slots dissoc this))))]
                                    (vswap! state-vol update :slots assoc cell {:component component
                                                                                :desc desc})
                                    cell)))}
                   {`component/instance :instance})))
    (advance [_ component _ opts]
      (let [state-vol (:state component)
            state @state-vol]
        (vreset!
          state-vol
          (-> state
              (assoc :opts opts)
              (update :slots
                      (fn [m]
                        (persistent!
                          (reduce-kv
                            (fn [acc k v]
                              (assoc! acc k (update v :component #(lifecycle/advance lifecycle/dynamic % (:desc v) opts))))
                            (transient m)
                            m))))))
        component))
    (delete [_ component opts]
      (let [{:keys [slots]} @(:state component)]
        (doseq [child (vals slots)]
          (lifecycle/delete lifecycle/dynamic (:component child) opts))))))

(def virtual-flow-props
  (merge
    fx.region/props
    (composite/props VirtualFlow
      :gravity [:setter lifecycle/scalar :coerce (coerce/enum VirtualFlow$Gravity) :default :front]
      :items [(mutator/observable-list #(-> ^VirtualFlow % .getProperties (.get ::items))) lifecycle/scalar]
      :orientation [mutator/forbidden lifecycle/scalar]
      :cell-factory [mutator/forbidden cell-factory-lifecycle])))

(def virtual-flow
  "Cljfx lifecycle describing VirtualFlow

  Supported props:

    :items          sequential collection of any objects
    :cell-factory   fn that converts item to cljfx description; required, can't be changed
    :orientation    item layout orientation, either :vertical (default) or :horizontal
    :gravity        cell alignment in the container, :front (default) or :rear
    ...all region props

  Example:

    {:fx/type fx.flowless/virtual-flow
     :cell-factory identity
     :items (for [i (range 1000)]
              {:fx/type :label :text (str i)})}"
  (composite/lifecycle
    {:props virtual-flow-props
     :args [:orientation :items :cell-factory]
     :ctor (fn [orientation items cell-factory]
             (let [items (FXCollections/observableArrayList ^Collection (or items []))
                   ^VirtualFlow node (case orientation
                                       (nil :vertical) (VirtualFlow/createVertical items cell-factory)
                                       :horizontal (VirtualFlow/createHorizontal items cell-factory))]
               (doto node (-> .getProperties (.put ::items items)))))}))

(def virtualized-scroll-pane-props
  (merge
    fx.region/props
    (composite/props VirtualizedScrollPane
      :content [mutator/forbidden lifecycle/dynamic]
      :hbar-policy [:setter lifecycle/scalar :coerce (coerce/enum ScrollPane$ScrollBarPolicy) :default :as-needed]
      :vbar-policy [:setter lifecycle/scalar :coerce (coerce/enum ScrollPane$ScrollBarPolicy) :default :as-needed])))

(def virtualized-scroll-pane
  "Cljfx lifecycle that wraps VirtualFlow and adds its scroll bars

  Supported props:

    :content        cljfx description that defines VirtualFlow; required, can't be changed
    :hbar-policy    horizontal scroll bar policy - :as-needed (default), :always or :never
    :vbar-policy    vertical scroll bar policy - :as-needed (default), :always or :never

  Example:

    {:fx/type fx.flowless/virtualized-scroll-pane
     :content {:fx/type fx.flowless/virtual-flow
               :cell-factory identity
               :items (for [i (range 1000)]
                        {:fx/type :label :text (str i)}}"
  (composite/describe VirtualizedScrollPane
    :ctor [:content]
    :props virtualized-scroll-pane-props))